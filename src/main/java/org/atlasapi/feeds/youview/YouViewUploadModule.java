package org.atlasapi.feeds.youview;

import static com.metabroadcast.common.time.DateTimeZones.UTC;
import static org.joda.time.DateTimeConstants.JANUARY;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.tvanytime.granular.GranularTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.granular.ValidatingGranularTvAnytimeGenerator;
import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmBroadcastServiceMapping;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmIdGenerator;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastServiceMapping;
import org.atlasapi.feeds.youview.payload.Converter;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.TVAPayloadCreator;
import org.atlasapi.feeds.youview.payload.TVAnytimeStringConverter;
import org.atlasapi.feeds.youview.persistence.MongoSentBroadcastEventPcridStore;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.SentBroadcastEventPcridStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.resolution.FullHierarchyResolvingContentResolver;
import org.atlasapi.feeds.youview.resolution.UpdatedContentResolver;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.feeds.youview.revocation.MongoRevokedContentStore;
import org.atlasapi.feeds.youview.revocation.OnDemandBasedRevocationProcessor;
import org.atlasapi.feeds.youview.revocation.RevocationProcessor;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsStore;
import org.atlasapi.feeds.youview.tasks.Destination.DestinationType;
import org.atlasapi.feeds.youview.tasks.creation.BootstrapTaskCreationTask;
import org.atlasapi.feeds.youview.tasks.creation.DeltaTaskCreationTask;
import org.atlasapi.feeds.youview.tasks.creation.TaskCreator;
import org.atlasapi.feeds.youview.tasks.creation.YouViewEntityTaskCreator;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.tasks.processing.HttpYouViewClient;
import org.atlasapi.feeds.youview.tasks.processing.PublisherDelegatingTaskProcessor;
import org.atlasapi.feeds.youview.tasks.processing.TaskProcessor;
import org.atlasapi.feeds.youview.tasks.processing.YouViewClient;
import org.atlasapi.feeds.youview.tasks.processing.YouViewTaskProcessor;
import org.atlasapi.feeds.youview.tasks.upload.UploadTask;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastServiceMapping;
import org.atlasapi.feeds.youview.unbox.UnboxIdGenerator;
import org.atlasapi.feeds.youview.upload.ReferentialIntegrityCheckingReportHandler;
import org.atlasapi.feeds.youview.upload.ResultHandler;
import org.atlasapi.feeds.youview.upload.TaskUpdatingResultHandler;
import org.atlasapi.feeds.youview.upload.YouViewReportHandler;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.xml.sax.SAXException;

import tva.metadata._2010.TVAMainType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;


// TODO this module is still a mess...
@Configuration
@Import(TVAnytimeFeedsModule.class)
public class YouViewUploadModule {
    
    private static final String CONFIG_PREFIX = "youview.upload.";
    
    private static final Map<String, Publisher> GRANULAR_PUBLISHER_MAPPING = ImmutableMap.of(
            "nitro", Publisher.BBC_NITRO,
            "lovefilm", Publisher.LOVEFILM,
            "unbox", Publisher.AMAZON_UNBOX
    );
    
    private static final RepetitionRule DELTA_UPLOAD = RepetitionRules.every(Duration.standardMinutes(30));
    private static final RepetitionRule BOOTSTRAP_UPLOAD = RepetitionRules.NEVER;
    private static final RepetitionRule REMOTE_CHECK_UPLOAD = RepetitionRules.every(Duration.standardMinutes(15));
    private static final RepetitionRule UPLOAD = RepetitionRules.every(Duration.standardMinutes(15));
    private static final RepetitionRule TASK_REMOVAL = RepetitionRules.daily(LocalTime.MIDNIGHT);
    private static final String TASK_NAME_PATTERN = "YouView %s TVAnytime %s Upload";
    private static final DestinationType DESTINATION_TYPE = DestinationType.YOUVIEW;
    private static final DateTime BOOTSTRAP_START_DATE = new DateTime(2015, JANUARY, 5, 0, 0, 0, 0, UTC);
    
    private final Clock clock = new SystemClock(DateTimeZone.UTC);
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired GranularTvAnytimeGenerator granularGenerator;
    private @Autowired TaskStore taskStore;
    private @Autowired BbcServiceIdResolver bbcServiceIdResolver;
    private @Autowired IdGenerator nitroIdGenerator;
    private @Autowired NitroBroadcastServiceMapping nitroBroadcastServiceMapping;
    private @Autowired LoveFilmIdGenerator loveFilmIdGenerator;
    private @Autowired LoveFilmBroadcastServiceMapping loveFilmServiceMapping;
    private @Autowired UnboxIdGenerator unboxIdGenerator;
    private @Autowired UnboxBroadcastServiceMapping unboxServiceMapping;
    private @Autowired BroadcastHierarchyExpander broadcastHierarchyExpander;
    private @Autowired OnDemandHierarchyExpander onDemandHierarchyExpander;
    private @Autowired VersionHierarchyExpander versionHierarchyExpander;
    private @Autowired ContentHierarchyExpander contentHierarchyExpander;
    private @Autowired FeedStatisticsStore feedStatsStore;
    private @Autowired ContentHierarchyExtractor contentHierarchy;
    
    private @Value("${youview.upload.validation}") String performValidation;
    private @Value("{youview.upload.maxRetries}") Integer maxRetries;
    private @Value("{youview.upload.taskTrimWindow.days}") Long trimWindowLengthDays;

    @PostConstruct
    public void startScheduledTasks() throws JAXBException, SAXException {
        for (Entry<String, Publisher> publisherEntry : GRANULAR_PUBLISHER_MAPPING.entrySet()) {
            String publisherPrefix = CONFIG_PREFIX + publisherEntry.getKey();
            if (isEnabled(publisherPrefix)) {
                scheduler.schedule(scheduleBootstrapTaskCreationTask(publisherEntry.getValue()), BOOTSTRAP_UPLOAD);
                scheduler.schedule(scheduleDeltaTaskCreationTask(publisherEntry.getValue()), DELTA_UPLOAD);
            }
        }
        
        scheduler.schedule(uploadTask().withName("YouView Task Upload"), UPLOAD);
        scheduler.schedule(remoteCheckTask().withName("YouView Task Status Check"), REMOTE_CHECK_UPLOAD);
        scheduler.schedule(taskTrimmingTask().withName("Old Task Removal"), TASK_REMOVAL);
        
        resultHandler().registerReportHandler(reportHandler());
    }
    
    private ScheduledTask remoteCheckTask() throws JAXBException, SAXException {
        return new RemoteCheckTask(taskStore, taskProcessor(), DESTINATION_TYPE);
    }
    
    private ScheduledTask taskTrimmingTask() {
        Duration taskTrimmingWindow = Duration.standardDays(trimWindowLengthDays);
        return new TaskTrimmingTask(taskStore, clock, taskTrimmingWindow);
    }

    @Bean
    public TaskProcessor taskProcessor() throws JAXBException, SAXException {
        ImmutableMap.Builder<Publisher, TaskProcessor> processors = ImmutableMap.builder();
        
        for (Entry<String, Publisher> publisherEntry : GRANULAR_PUBLISHER_MAPPING.entrySet()) {
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
            Optional.absent();
        }
        String baseUrl = parseUrl(publisherPrefix);
        UsernameAndPassword credentials = parseCredentials(publisherPrefix);
        
        YouViewClient client = new HttpYouViewClient(httpClient(credentials.username(), credentials.password()), baseUrl, clock);
        
        return Optional.<TaskProcessor>of(new YouViewTaskProcessor(client, resultHandler(), revokedContentStore()));
    }

    @Bean
    public YouViewUploadController uploadController() throws JAXBException, SAXException {
        return new YouViewUploadController(contentResolver, taskCreator(), taskStore, contentHierarchyExpander, revocationProcessor());
    }
    
    private ScheduledTask uploadTask() throws JAXBException, SAXException {
        return new UploadTask(taskStore, taskProcessor(), DESTINATION_TYPE);
    }
    
    // TODO remove dependency on nitro Id generator
    private ScheduledTask scheduleBootstrapTaskCreationTask(Publisher publisher) 
            throws JAXBException, SAXException {
        return new BootstrapTaskCreationTask(
                lastUpdatedStore(), 
                publisher, 
                contentHierarchyExpander, 
                nitroIdGenerator, 
                taskStore, 
                taskCreator(), 
                payloadCreator(), 
                nitroBootstrapContentResolver(publisher), 
                BOOTSTRAP_START_DATE
        )
        .withName(String.format(TASK_NAME_PATTERN, "Bootstrap", publisher.title()));
    }
    
    private ScheduledTask scheduleDeltaTaskCreationTask(Publisher publisher) 
            throws JAXBException, SAXException {
        return new DeltaTaskCreationTask(
                lastUpdatedStore(), 
                publisher, 
                contentHierarchyExpander, 
                nitroIdGenerator, 
                taskStore, 
                taskCreator(), 
                payloadCreator(), 
                nitroDeltaContentResolver(publisher)
        )
        .withName(String.format(TASK_NAME_PATTERN, "Delta", publisher.title()));
    }
    
    private PayloadCreator payloadCreator() throws JAXBException, SAXException {
        Converter<JAXBElement<TVAMainType>, String> outputConverter = new TVAnytimeStringConverter();
        GranularTvAnytimeGenerator generator = enableValidationIfAppropriate(granularGenerator);
        return new TVAPayloadCreator(generator, outputConverter, sentBroadcastProgramUrlStore(), clock);
    }
    
    // TODO this could move to the TVAnytimeFeedsModule
    private GranularTvAnytimeGenerator enableValidationIfAppropriate(
            GranularTvAnytimeGenerator granularGenerator) throws JAXBException, SAXException {
        
        if (Boolean.parseBoolean(performValidation)) {
            granularGenerator = new ValidatingGranularTvAnytimeGenerator(granularGenerator);
        }
        return granularGenerator;
    }
    
    private YouViewContentResolver nitroBootstrapContentResolver(Publisher publisher) {
        return new FullHierarchyResolvingContentResolver(
                new UpdatedContentResolver(contentFinder, publisher), 
                contentHierarchy
        );
    }
    
    private YouViewContentResolver nitroDeltaContentResolver(Publisher publisher) {
        return new UpdatedContentResolver(contentFinder, publisher);
    }
     
    @Bean
    public YouViewLastUpdatedStore lastUpdatedStore() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
    
    @Bean
    public ResultHandler resultHandler() throws JAXBException, SAXException {
        return new TaskUpdatingResultHandler(taskStore, maxRetries);
    }

    @Bean
    public YouViewReportHandler reportHandler() throws JAXBException, SAXException {
        return new ReferentialIntegrityCheckingReportHandler(
                taskCreator(), 
                nitroIdGenerator, 
                taskStore, 
                contentResolver, 
                versionHierarchyExpander, 
                contentHierarchy
        );
    }

    private TaskCreator taskCreator() {
        return new YouViewEntityTaskCreator();
    }

    @Bean
    public RevocationProcessor revocationProcessor() throws JAXBException, SAXException {
        return new OnDemandBasedRevocationProcessor(revokedContentStore(), onDemandHierarchyExpander, taskCreator(), taskStore);
    }
    
    @Bean
    public SentBroadcastEventPcridStore sentBroadcastProgramUrlStore() {
        return new MongoSentBroadcastEventPcridStore(mongo);
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
    
    private SimpleHttpClient httpClient(String username, String password) {
        return new SimpleHttpClientBuilder()
            .withHeader("Content-Type", "text/xml")
            .withSocketTimeout(1, TimeUnit.MINUTES)
            .withPreemptiveBasicAuth(new UsernameAndPassword(username, password))
            .build();
    }
}
