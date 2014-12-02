package org.atlasapi.feeds.youview;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.granular.GranularTvAnytimeGenerator;
import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmBroadcastServiceMapping;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmIdGenerator;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastServiceMapping;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.resolution.AvailableContentResolver;
import org.atlasapi.feeds.youview.resolution.UpdatedContentResolver;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.feeds.youview.revocation.MongoRevokedContentStore;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsStore;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastServiceMapping;
import org.atlasapi.feeds.youview.unbox.UnboxIdGenerator;
import org.atlasapi.feeds.youview.upload.BootstrapUploadTask;
import org.atlasapi.feeds.youview.upload.DefaultGranularYouViewService;
import org.atlasapi.feeds.youview.upload.DefaultYouViewService;
import org.atlasapi.feeds.youview.upload.DeltaUploadTask;
import org.atlasapi.feeds.youview.upload.HttpYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.PublisherDelegatingGranularYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.PublisherDelegatingYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.ValidatingYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.YouViewService;
import org.atlasapi.feeds.youview.upload.granular.GranularBootstrapUploadTask;
import org.atlasapi.feeds.youview.upload.granular.GranularDeltaUploadTask;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.xml.sax.SAXException;

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
    private static final Map<String, Publisher> PUBLISHER_MAPPING = ImmutableMap.of(
            "lovefilm", Publisher.LOVEFILM,
            "unbox", Publisher.AMAZON_UNBOX
    );
    
    private static final Map<String, Publisher> GRANULAR_PUBLISHER_MAPPING = ImmutableMap.of(
            "nitro", Publisher.BBC_NITRO
    );
    
    private final static RepetitionRule DELTA_UPLOAD = RepetitionRules.NEVER;//RepetitionRules.every(Duration.standardHours(12)).withOffset(Duration.standardHours(10));
    private final static RepetitionRule BOOTSTRAP_UPLOAD = RepetitionRules.NEVER;
    private final static RepetitionRule REMOTE_CHECK_UPLOAD = RepetitionRules.every(Duration.standardMinutes(15));
    private static final String TASK_NAME_PATTERN = "YouView %s TVAnytime %s Upload";
    
    private final Clock clock = new SystemClock(DateTimeZone.UTC);
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired TvAnytimeGenerator generator;
    private @Autowired FeedStatisticsStore feedStatsStore;
    private @Autowired GranularTvAnytimeGenerator granularGenerator;
    private @Autowired TaskStore taskStore;
    private @Autowired BbcServiceIdResolver bbcServiceIdResolver;
    private @Autowired NitroIdGenerator nitroIdGenerator;
    private @Autowired NitroBroadcastServiceMapping nitroBroadcastServiceMapping;
    private @Autowired LoveFilmIdGenerator loveFilmIdGenerator;
    private @Autowired LoveFilmBroadcastServiceMapping loveFilmServiceMapping;
    private @Autowired UnboxIdGenerator unboxIdGenerator;
    private @Autowired UnboxBroadcastServiceMapping unboxServiceMapping;
    private @Autowired BroadcastHierarchyExpander broadcastHierarchyExpander;
    private @Autowired OnDemandHierarchyExpander onDemandHierarchyExpander;
    private @Autowired ContentHierarchyExpander contentHierarchyExpander;
    
    private @Value("${youview.upload.validation}") String performValidation;

    @PostConstruct
    public void startScheduledTasks() throws JAXBException, SAXException {
        // TODO this duplicates the loop instantiating youview clients - fix this
        for (Entry<String, Publisher> publisherEntry : PUBLISHER_MAPPING.entrySet()) {
            String publisherPrefix = CONFIG_PREFIX + publisherEntry.getKey();
            if (isEnabled(publisherPrefix)) {
                scheduler.schedule(scheduleBootstrapTask(publisherEntry.getValue()), BOOTSTRAP_UPLOAD);
                scheduler.schedule(scheduleDeltaTask(publisherEntry.getValue()), DELTA_UPLOAD);
                
            }
        }
        for (Entry<String, Publisher> publisherEntry : GRANULAR_PUBLISHER_MAPPING.entrySet()) {
            String publisherPrefix = CONFIG_PREFIX + publisherEntry.getKey();
            if (isEnabled(publisherPrefix)) {
                scheduler.schedule(scheduleGranularBootstrapTask(publisherEntry.getValue()), BOOTSTRAP_UPLOAD);
                scheduler.schedule(scheduleGranularDeltaTask(publisherEntry.getValue()), DELTA_UPLOAD);
                
            }
        }
        
        scheduler.schedule(remoteCheckTask().withName("YouView Task Status Check"), REMOTE_CHECK_UPLOAD);
    }
    
    private ScheduledTask remoteCheckTask() throws JAXBException, SAXException {
        return new YouViewRemoteCheckTask(taskStore, granularYouViewUploadClient());
    }

    @Bean
    public YouViewUploadController uploadController() throws JAXBException, SAXException {
        return new YouViewUploadController(contentResolver, granularYouViewUploadClient(), contentHierarchyExpander);
    }
    
    private ScheduledTask scheduleBootstrapTask(Publisher publisher) throws JAXBException, SAXException {
        return new BootstrapUploadTask(youViewUploadClient(), lastUpdatedStore(), publisher, nitroContentResolver(publisher))
                    .withName(String.format(TASK_NAME_PATTERN, "Bootstrap", publisher.title()));
    }

    private ScheduledTask scheduleDeltaTask(Publisher publisher) throws JAXBException, SAXException {
        return new DeltaUploadTask(youViewUploadClient(), lastUpdatedStore(), publisher, nitroContentResolver(publisher))
                    .withName(String.format(TASK_NAME_PATTERN, "Delta", publisher.title()));
    }
    
    private ScheduledTask scheduleGranularBootstrapTask(Publisher publisher) throws JAXBException, SAXException {
        return new GranularBootstrapUploadTask(
                granularYouViewUploadClient(), 
                lastUpdatedStore(), 
                publisher, 
                nitroContentResolver(publisher), 
                contentHierarchyExpander, 
                nitroIdGenerator
        )
        .withName(String.format(TASK_NAME_PATTERN, "Bootstrap", publisher.title()));
    }

    private ScheduledTask scheduleGranularDeltaTask(Publisher publisher) throws JAXBException, SAXException {
        return new GranularDeltaUploadTask(
                granularYouViewUploadClient(), 
                lastUpdatedStore(), 
                publisher, 
                nitroContentResolver(publisher), 
                contentHierarchyExpander, 
                nitroIdGenerator
        )
        .withName(String.format(TASK_NAME_PATTERN, "Delta", publisher.title()));
    }
    
    private YouViewContentResolver nitroContentResolver(Publisher publisher) {
        return new AvailableContentResolver(new UpdatedContentResolver(contentFinder, publisher));
    }
            

    @Bean
    public YouViewLastUpdatedStore lastUpdatedStore() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
    
    @Bean
    public GranularYouViewService granularYouViewUploadClient() throws JAXBException, SAXException {
        ImmutableMap.Builder<Publisher, GranularYouViewService> clients = ImmutableMap.builder();
        
        Optional<GranularYouViewService> nitroClient = createNitroClient();
        if (nitroClient.isPresent()) {
            clients.put(Publisher.BBC_NITRO, nitroClient.get());
        }
        
        return new PublisherDelegatingGranularYouViewRemoteClient(clients.build());
    }
    
    @Bean
    public YouViewService youViewUploadClient() throws JAXBException, SAXException {
        ImmutableMap.Builder<Publisher, YouViewService> clients = ImmutableMap.builder();
        
        Optional<YouViewService> loveFilmClient = createLoveFilmClient();
        if (loveFilmClient.isPresent()) {
            clients.put(Publisher.LOVEFILM, loveFilmClient.get());
        }
        Optional<YouViewService> unboxClient = createUnboxClient();
        if (unboxClient.isPresent()) {
            clients.put(Publisher.AMAZON_UNBOX, unboxClient.get());
        }
        
        return new PublisherDelegatingYouViewRemoteClient(clients.build());
    }

    private Optional<GranularYouViewService> createNitroClient() throws JAXBException, SAXException {
        String publisherPrefix = CONFIG_PREFIX + "nitro";
        if (!isEnabled(publisherPrefix)) {
            Optional.absent();
        }
        String baseUrl = parseUrl(publisherPrefix);
        UsernameAndPassword credentials = parseCredentials(publisherPrefix);
        
        YouViewRemoteClient client = new HttpYouViewRemoteClient(httpClient(credentials.username(), credentials.password()), baseUrl);
        client = enableValidationIfAppropriate(client);
        
        return Optional.<GranularYouViewService>of(new DefaultGranularYouViewService(
                granularGenerator, 
                nitroIdGenerator, 
                clock, 
                revokedContentStore(), 
                client, 
                taskStore,
                onDemandHierarchyExpander
        ));
    }

    private YouViewRemoteClient enableValidationIfAppropriate(YouViewRemoteClient client) throws JAXBException,
            SAXException {
        if (Boolean.parseBoolean(performValidation)) {
            client = new ValidatingYouViewRemoteClient(client);
        }
        return client;
    }
    
    private Optional<YouViewService> createLoveFilmClient() throws JAXBException, SAXException {
        String publisherPrefix = CONFIG_PREFIX + "lovefilm";
        if (!isEnabled(publisherPrefix)) {
            Optional.absent();
        }
        UsernameAndPassword credentials = parseCredentials(publisherPrefix);
        
        YouViewRemoteClient client = new HttpYouViewRemoteClient(httpClient(credentials.username(), credentials.password()), parseUrl(publisherPrefix));
        client = enableValidationIfAppropriate(client);
        
        return Optional.<YouViewService>of(new DefaultYouViewService(
                generator, 
                loveFilmIdGenerator, 
                clock, 
                revokedContentStore(), 
                client, 
                taskStore, 
                broadcastHierarchyExpander,
                onDemandHierarchyExpander
        ));
    }
    
    private Optional<YouViewService> createUnboxClient() throws JAXBException, SAXException {
        String publisherPrefix = CONFIG_PREFIX + "unbox";
        if (!isEnabled(publisherPrefix)) {
            Optional.absent();
        }
        UsernameAndPassword credentials = parseCredentials(publisherPrefix);
        
        YouViewRemoteClient client = new HttpYouViewRemoteClient(httpClient(credentials.username(), credentials.password()), parseUrl(publisherPrefix));
        client = enableValidationIfAppropriate(client);      

        return Optional.<YouViewService>of(new DefaultYouViewService(
                generator, 
                unboxIdGenerator, 
                clock, 
                revokedContentStore(), 
                client, 
                taskStore, 
                broadcastHierarchyExpander,
                onDemandHierarchyExpander
        ));
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
