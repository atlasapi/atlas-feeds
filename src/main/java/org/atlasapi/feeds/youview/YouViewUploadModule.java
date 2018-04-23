package org.atlasapi.feeds.youview;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.checking.RemoteCheckTask;
import org.atlasapi.feeds.tasks.maintainance.TaskTrimmingTask;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.creation.BootstrapTaskCreationTask;
import org.atlasapi.feeds.tasks.youview.creation.DeltaTaskCreationTask;
import org.atlasapi.feeds.tasks.youview.creation.RepresentativeIdChangesHandlingTask;
import org.atlasapi.feeds.tasks.youview.creation.TaskCreator;
import org.atlasapi.feeds.tasks.youview.creation.YouViewEntityTaskCreator;
import org.atlasapi.feeds.tasks.youview.processing.DeleteTask;
import org.atlasapi.feeds.tasks.youview.processing.PublisherDelegatingTaskProcessor;
import org.atlasapi.feeds.tasks.youview.processing.TaskProcessor;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.tasks.youview.processing.YouViewTaskProcessor;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.ValidatingTvAnytimeGenerator;
import org.atlasapi.feeds.youview.client.HttpYouViewClient;
import org.atlasapi.feeds.youview.client.ReferentialIntegrityCheckingReportHandler;
import org.atlasapi.feeds.youview.client.ResultHandler;
import org.atlasapi.feeds.youview.client.TaskUpdatingResultHandler;
import org.atlasapi.feeds.youview.client.YouViewClient;
import org.atlasapi.feeds.youview.client.YouViewReportHandler;
import org.atlasapi.feeds.youview.payload.Converter;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.TVAPayloadCreator;
import org.atlasapi.feeds.youview.payload.TVAnytimeStringConverter;
import org.atlasapi.feeds.youview.persistence.BroadcastEventDeduplicator;
import org.atlasapi.feeds.youview.persistence.MongoSentBroadcastEventPcridStore;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.MongoYouViewPayloadHashStore;
import org.atlasapi.feeds.youview.persistence.RollingWindowBroadcastEventDeduplicator;
import org.atlasapi.feeds.youview.persistence.SentBroadcastEventPcridStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.FullHierarchyResolvingContentResolver;
import org.atlasapi.feeds.youview.resolution.UpdatedContentResolver;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.feeds.youview.revocation.MongoRevokedContentStore;
import org.atlasapi.feeds.youview.revocation.OnDemandBasedRevocationProcessor;
import org.atlasapi.feeds.youview.revocation.RevocationProcessor;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.www.MetricsController;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;

import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;

import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.xml.sax.SAXException;
import tva.metadata._2010.TVAMainType;

import static com.metabroadcast.common.time.DateTimeZones.UTC;
import static java.lang.management.ManagementFactory.getGarbageCollectorMXBeans;
import static org.joda.time.DateTimeConstants.OCTOBER;

/**
 * This Module wraps all code concerning upload of YouView TVAnytime feeds, including identification
 * of content to upload, conversion of this into Tasks, and upload of the payloads of those Tasks to
 * a YouView environment, and status checking of those transactions.
 * <p>
 * This module also instantiates a controller for manual upload/deletion/revocation of particular 
 * TVAnytime elements/Atlas pieces of content to/from the YouView environment. 
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
@Configuration
@Import({TVAnytimeFeedsModule.class, ContentHierarchyExpanderFactory.class})
public class YouViewUploadModule {
    private static final Logger log = LoggerFactory.getLogger(YouViewUploadModule.class);

    private static final String CONFIG_PREFIX = "youview.upload.";

    private static final Map<String, Publisher> PUBLISHER_MAPPING = ImmutableMap.of(
            "nitro", Publisher.BBC_NITRO,
            "unbox", Publisher.AMAZON_UNBOX
    );

    private static final RepetitionRule NITRO_DELTA_CONTENT_CHECK = RepetitionRules.every(Duration.standardMinutes(2));
    private static final RepetitionRule AMAZON_DELTA_CONTENT_CHECK = RepetitionRules.daily(LocalTime.MIDNIGHT.plusHours(12));
    private static final RepetitionRule REPID_CHANGES_HANDLING = RepetitionRules.NEVER;
private static final RepetitionRule BOOTSTRAP_CONTENT_CHECK = RepetitionRules.NEVER;
    private static final RepetitionRule REMOTE_CHECK = RepetitionRules.every(Duration.standardHours(1));
    // Uploads are being performed as part of the delta job.
    private static final RepetitionRule UPLOAD = RepetitionRules.NEVER;
    private static final RepetitionRule DELETE = RepetitionRules.every(Duration.standardMinutes(15)).withOffset(Duration.standardMinutes(5));
    private static final RepetitionRule TASK_REMOVAL = RepetitionRules.daily(LocalTime.MIDNIGHT);
    
    private static final String TASK_NAME_PATTERN = "YouView %s TVAnytime %s Upload";
    private static final DestinationType DESTINATION_TYPE = DestinationType.YOUVIEW;
    private static final DateTime BOOTSTRAP_START_DATE = new DateTime(2017, OCTOBER, 11, 0, 0, 0, 0, UTC);

    private final Clock clock = new SystemClock(DateTimeZone.UTC);

    private MetricRegistry metrics = new MetricRegistry();
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired TvAnytimeGenerator generator;
    private @Autowired TaskStore taskStore;
    private @Autowired ContentHierarchyExtractor contentHierarchy;
    private @Autowired ChannelResolver channelResolver;
    private @Autowired @Qualifier("YouviewQueryExecutor") KnownTypeQueryExecutor mergingResolver;
    private @Autowired ScheduleResolver scheduleResolver;
    private @Autowired ContentHierarchyExpanderFactory contentHierarchyExpanderFactory;

    private @Value("${youview.upload.validation}") String performValidation;

    @PostConstruct
    public void startScheduledTasks() throws JAXBException, SAXException {
        for (Entry<String, Publisher> publisherEntry : PUBLISHER_MAPPING.entrySet()) {
            String publisherPrefix = CONFIG_PREFIX + publisherEntry.getKey();
            if (isEnabled(publisherPrefix)) {
                scheduler.schedule(scheduleBootstrapTaskCreationTask(publisherEntry.getValue()), BOOTSTRAP_CONTENT_CHECK);

                if(publisherEntry.getValue().equals(Publisher.BBC_NITRO)){
                    scheduler.schedule(scheduleDeltaTaskCreationTask(publisherEntry.getValue()), NITRO_DELTA_CONTENT_CHECK);
                }
                else if(publisherEntry.getValue().equals(Publisher.AMAZON_UNBOX)){
                    scheduler.schedule(scheduleDeltaTaskCreationTask(publisherEntry.getValue()), AMAZON_DELTA_CONTENT_CHECK);
                    scheduler.schedule(scheduleRepIdChangesHandlingTask(Publisher.AMAZON_UNBOX), REPID_CHANGES_HANDLING);
                }
            }
        }

        scheduler.schedule(uploadTask().withName("YouView Uploads"), UPLOAD);
        scheduler.schedule(deleteTask().withName("YouView Deletes"), DELETE);
        scheduler.schedule(remoteCheckTask().withName("YouView Task Status Check"), REMOTE_CHECK);
        scheduler.schedule(taskTrimmingTask().withName("Old Task Removal"), TASK_REMOVAL);
        
        resultHandler().registerReportHandler(reportHandler());
    }
    
    private ScheduledTask remoteCheckTask() throws JAXBException, SAXException {
        return new RemoteCheckTask(taskStore, taskProcessor(), DESTINATION_TYPE);
    }
    
    private ScheduledTask taskTrimmingTask() {
        Duration taskTrimmingWindow = Duration.standardDays(Configurer.get("youview.upload.taskTrimWindow.days").toLong());
        return new TaskTrimmingTask(taskStore, clock, taskTrimmingWindow);
    }

    @Bean
    public TaskProcessor taskProcessor() throws JAXBException, SAXException {
        ImmutableMap.Builder<Publisher, TaskProcessor> processors = ImmutableMap.builder();
        
        for (Entry<String, Publisher> publisherEntry : PUBLISHER_MAPPING.entrySet()) {
            Optional<TaskProcessor> processor = taskProcessor(publisherEntry.getKey());
            if (processor.isPresent()) {
                processors.put(publisherEntry.getValue(), processor.get());
            }    
        }
        
        return new PublisherDelegatingTaskProcessor(processors.build());
    }
    
    /**
     * Creates a {@link TaskProcessor} for the specified publisher prefix, if that publisher
     * is enabled, wrapping it within an {@link Optional}. 
     * @return Optional.absent() if configuration disabled, otherwise returns the processor 
     * (wrapped in an Optional) if configuration is present.
     * @throws SAXException 
     * @throws JAXBException 
     */
    private Optional<TaskProcessor> taskProcessor(String prefix) throws JAXBException, SAXException {
        String publisherPrefix = CONFIG_PREFIX + prefix;
        if (!isEnabled(publisherPrefix)) {
            return Optional.absent();
        }


        String baseUrl = parseUrl(publisherPrefix);
        final UsernameAndPassword credentials = parseCredentials(publisherPrefix);

        HttpTransport transport = new NetHttpTransport();
        HttpRequestFactory requestFactory = transport.createRequestFactory(
                new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) throws IOException {
                        request.setThrowExceptionOnExecuteError(false)
                                .getHeaders()
                                .setBasicAuthentication(
                                        credentials.username(),
                                        credentials.password()
                                ).setContentType(MimeType.TEXT_XML.toString());
                    }
                });

        YouViewClient client = new HttpYouViewClient(
                requestFactory,
                baseUrl,
                clock
        );
        
        return Optional.<TaskProcessor>of(new YouViewTaskProcessor(client, resultHandler(), revokedContentStore(), taskStore));
    }

    @Bean
    public YouViewUploadController uploadController() throws JAXBException, SAXException {
        return YouViewUploadController.builder()
                .withContentResolver(contentResolver)
                .withTaskCreator(taskCreator())
                .withTaskStore(taskStore)
                .withPayloadCreator(payloadCreator(new BroadcastEventDeduplicator() {

                    @Override
                    public boolean shouldUpload(JAXBElement<TVAMainType> broadcastEventTva) {
                        // Force upload needs to always upload things, no dodgy excuses.
                        return true;
                    }

                    @Override
                    public void recordUpload(
                            JAXBElement<TVAMainType> broadcastEventTva,
                            Broadcast broadcast
                    ) {
                        // nope.
                    }
                }))
                .withRevocationProcessor(revocationProcessor())
                .withTaskProcessor(taskProcessor())
                .withScheduleResolver(scheduleResolver)
                .withChannelResolver(channelResolver)
                .withClock(clock)
                .build();
    }

    @Bean
    public YouViewPayloadHashStore payloadHashStore() {
        return new MongoYouViewPayloadHashStore(mongo);
    }

    private UpdateTask uploadTask() throws JAXBException, SAXException {
        return new UpdateTask(taskStore, taskProcessor(), null, DESTINATION_TYPE); //null for all publishers
    }

    private UpdateTask uploadTask(Publisher publisher) throws JAXBException, SAXException {
        return new UpdateTask(taskStore, taskProcessor(), publisher, DESTINATION_TYPE);
    }
    
    private ScheduledTask deleteTask() throws JAXBException, SAXException {
        return new DeleteTask(taskStore, taskProcessor(), null, DESTINATION_TYPE);
    }

    private ScheduledTask scheduleBootstrapTaskCreationTask(Publisher publisher) 
            throws JAXBException, SAXException {
        return new BootstrapTaskCreationTask(
                lastUpdatedStore(), 
                publisher,
                contentHierarchyExpanderFactory.create(publisher),
                IdGeneratorFactory.create(publisher),
                taskStore, 
                taskCreator(), 
                payloadCreator(), 
                getBootstrapContentResolver(publisher),
                payloadHashStore(),
                BOOTSTRAP_START_DATE
        )
        .withName(String.format(TASK_NAME_PATTERN, "Bootstrap", publisher.title()));
    }
    
    private ScheduledTask scheduleDeltaTaskCreationTask(Publisher publisher) 
            throws JAXBException, SAXException {
        return new DeltaTaskCreationTask(
                lastUpdatedStore(), 
                publisher,
                contentHierarchyExpanderFactory.create(publisher),
                IdGeneratorFactory.create(publisher),
                taskStore, 
                taskCreator(), 
                payloadCreator(), 
                uploadTask(publisher),
                getDeltaContentResolver(publisher),
                payloadHashStore(),
                channelResolver,
                mergingResolver
        )
        .withName(String.format(TASK_NAME_PATTERN, "Delta", publisher.title()));
    }

    /**
     * Checks the representativeId service for changes and creates new tasks to handle the changes.
     * @param publisher
     * @return
     * @throws JAXBException
     * @throws SAXException
     */
    private ScheduledTask scheduleRepIdChangesHandlingTask(Publisher publisher)
            throws JAXBException, SAXException {
        return new RepresentativeIdChangesHandlingTask(
                lastUpdatedStore(),
                publisher,
                contentHierarchyExpanderFactory.create(publisher),
                IdGeneratorFactory.create(publisher),
                taskStore,
                taskCreator(),
                payloadCreator(),
                uploadTask(publisher),
                getDeltaContentResolver(publisher),
                payloadHashStore(),
                channelResolver,
                mergingResolver
        ).withName(String.format("YouView representativeId changes handling for %s", publisher.title()));
    }


    
    private PayloadCreator payloadCreator() throws JAXBException, SAXException {
        return payloadCreator(rollingWindowBroadcastEventDeduplicator());
    }

    private PayloadCreator payloadCreator(BroadcastEventDeduplicator broadcastEventDeduplicator)
            throws JAXBException, SAXException {
        Converter<JAXBElement<TVAMainType>, String> outputConverter = new TVAnytimeStringConverter();
        TvAnytimeGenerator tvaGenerator = enableValidationIfAppropriate(generator);
        return new TVAPayloadCreator(
                tvaGenerator,
                channelResolver,
                outputConverter,
                broadcastEventDeduplicator,
                clock
        );
    }

    private TvAnytimeGenerator enableValidationIfAppropriate(TvAnytimeGenerator generator)
            throws JAXBException, SAXException {
        
        if (Boolean.parseBoolean(performValidation)) {
            generator = new ValidatingTvAnytimeGenerator(generator);
        }
        return generator;
    }
    
    private YouViewContentResolver getBootstrapContentResolver(Publisher publisher) {
        return new FullHierarchyResolvingContentResolver(
                getDeltaContentResolver(publisher),
                contentHierarchy
        );
    }
    
    private YouViewContentResolver getDeltaContentResolver(Publisher publisher) {
        return new UpdatedContentResolver(contentFinder, contentResolver, publisher);
    }
     
    @Bean
    public YouViewLastUpdatedStore lastUpdatedStore() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
    
    @Bean
    public ResultHandler resultHandler() throws JAXBException, SAXException {
        return new TaskUpdatingResultHandler(taskStore, metrics);
    }

    @Bean
    public YouViewReportHandler reportHandler() throws JAXBException, SAXException {
        return new ReferentialIntegrityCheckingReportHandler(
                taskCreator(),
                taskStore,
                payloadHashStore(),
                payloadCreator(),
                contentResolver,
                contentHierarchy
        );
    }

    private TaskCreator taskCreator() {
        return new YouViewEntityTaskCreator(clock);
    }

    @Bean
    public RevocationProcessor revocationProcessor() throws JAXBException, SAXException {
        return new OnDemandBasedRevocationProcessor(revokedContentStore(), payloadCreator(), taskCreator(), taskStore);
    }
    
    @Bean
    public SentBroadcastEventPcridStore sentBroadcastProgramUrlStore() {
        return new MongoSentBroadcastEventPcridStore(mongo);
    }

    @Bean
    public RollingWindowBroadcastEventDeduplicator rollingWindowBroadcastEventDeduplicator(){
        return new RollingWindowBroadcastEventDeduplicator(sentBroadcastProgramUrlStore());
    }
    
    @Bean
    public RevokedContentStore revokedContentStore() {
        return new MongoRevokedContentStore(mongo);
    }

    private boolean isEnabled(String publisherPrefix) {
        return Boolean.parseBoolean(Configurer.get(publisherPrefix + ".upload.enabled").get());
    }

    private String parseUrl(String publisherPrefix) {
        return Configurer.get(publisherPrefix + ".url").get();
    }
    
    private UsernameAndPassword parseCredentials(String publisherPrefix) {
        return new UsernameAndPassword(
                Configurer.get(publisherPrefix + ".username").get(), 
                Configurer.get(publisherPrefix + ".password").get()
        );
    }

    @Bean
    public MetricsController metricsController() {
        CollectorRegistry collectorRegistry = new CollectorRegistry();

        metrics.registerAll(
                new GarbageCollectorMetricSet(
                        getGarbageCollectorMXBeans()
                )
        );
        metrics.registerAll(new MemoryUsageGaugeSet());
        metrics.registerAll(new ThreadStatesGaugeSet());
        metrics.registerAll(new JvmAttributeGaugeSet());

        collectorRegistry.register(new DropwizardExports(metrics));

        return MetricsController.create(collectorRegistry);
    }
}
