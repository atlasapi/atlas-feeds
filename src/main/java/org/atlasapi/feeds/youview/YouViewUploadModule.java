package org.atlasapi.feeds.youview;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmBroadcastServiceMapping;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmIdGenerator;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastServiceMapping;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.revocation.MongoRevokedContentStore;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastServiceMapping;
import org.atlasapi.feeds.youview.unbox.UnboxIdGenerator;
import org.atlasapi.feeds.youview.upload.BootstrapUploadTask;
import org.atlasapi.feeds.youview.upload.DeltaUploadTask;
import org.atlasapi.feeds.youview.upload.HttpYouViewClient;
import org.atlasapi.feeds.youview.upload.HttpYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.PublisherDelegatingYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.ValidatingYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.YouViewClient;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
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

@Configuration
@Import(TVAnytimeFeedsModule.class)
public class YouViewUploadModule {
    
    private static final String CONFIG_PREFIX = "youview.upload.";
    private static final Map<String, Publisher> PUBLISHER_MAPPING = ImmutableMap.of(
            "lovefilm", Publisher.LOVEFILM,
            "unbox", Publisher.AMAZON_UNBOX,
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
    
    private @Value("${youview.upload.validation}") String performValidation;

    // TODO this module is still a mess...
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
        scheduler.schedule(remoteCheckTask().withName("YouView Task Status Check"), REMOTE_CHECK_UPLOAD);
    }
    
    private ScheduledTask remoteCheckTask() throws JAXBException, SAXException {
        return new YouViewRemoteCheckTask(taskStore, youViewUploadClient());
    }

    @Bean
    public YouViewUploadController uploadController() throws JAXBException, SAXException {
        return new YouViewUploadController(contentResolver, youViewUploadClient());
    }
    
    private ScheduledTask scheduleBootstrapTask(Publisher publisher) throws JAXBException, SAXException {
        return new BootstrapUploadTask(youViewUploadClient(), contentFinder, lastUpdatedStore(), publisher)
                    .withName(String.format(TASK_NAME_PATTERN, "Bootstrap", publisher.title()));
    }
    
    private ScheduledTask scheduleDeltaTask(Publisher publisher) throws JAXBException, SAXException {
        return new DeltaUploadTask(youViewUploadClient(), contentFinder, lastUpdatedStore(), publisher)
                    .withName(String.format(TASK_NAME_PATTERN, "Delta", publisher.title()));
    }

    public @Bean YouViewLastUpdatedStore lastUpdatedStore() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
    
    @Bean
    public YouViewClient youViewUploadClient() throws JAXBException, SAXException {
        ImmutableMap.Builder<Publisher, YouViewClient> clients = ImmutableMap.builder();
        Optional<YouViewClient> nitroClient = createNitroClient();
        if (nitroClient.isPresent()) {
            clients.put(Publisher.BBC_NITRO, nitroClient.get());
        }
        Optional<YouViewClient> loveFilmClient = createLoveFilmClient();
        if (loveFilmClient.isPresent()) {
            clients.put(Publisher.LOVEFILM, loveFilmClient.get());
        }
        Optional<YouViewClient> unboxClient = createUnboxClient();
        if (unboxClient.isPresent()) {
            clients.put(Publisher.AMAZON_UNBOX, unboxClient.get());
        }
        
        return new PublisherDelegatingYouViewRemoteClient(clients.build());
    }

    private Optional<YouViewClient> createNitroClient() throws JAXBException, SAXException {
        String publisherPrefix = CONFIG_PREFIX + "nitro";
        if (!isEnabled(publisherPrefix)) {
            Optional.absent();
        }
        String baseUrl = parseUrl(publisherPrefix);
        UsernameAndPassword credentials = parseCredentials(publisherPrefix);
        
        YouViewRemoteClient client = new HttpYouViewRemoteClient(httpClient(credentials.username(), credentials.password()), baseUrl);
        client = enableValidationIfAppropriate(client);
        
        return Optional.<YouViewClient>of(new HttpYouViewClient(
                generator, 
                nitroIdGenerator, 
                clock, 
                revokedContentStore(), 
                client, 
                taskStore,
                broadcastHierarchyExpander,
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
    
    private Optional<YouViewClient> createLoveFilmClient() throws JAXBException, SAXException {
        String publisherPrefix = CONFIG_PREFIX + "lovefilm";
        if (!isEnabled(publisherPrefix)) {
            Optional.absent();
        }
        UsernameAndPassword credentials = parseCredentials(publisherPrefix);
        
        YouViewRemoteClient client = new HttpYouViewRemoteClient(httpClient(credentials.username(), credentials.password()), parseUrl(publisherPrefix));
        client = enableValidationIfAppropriate(client);
        
        return Optional.<YouViewClient>of(new HttpYouViewClient(
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
    
    private Optional<YouViewClient> createUnboxClient() throws JAXBException, SAXException {
        String publisherPrefix = CONFIG_PREFIX + "unbox";
        if (!isEnabled(publisherPrefix)) {
            Optional.absent();
        }
        UsernameAndPassword credentials = parseCredentials(publisherPrefix);
        
        YouViewRemoteClient client = new HttpYouViewRemoteClient(httpClient(credentials.username(), credentials.password()), parseUrl(publisherPrefix));
        client = enableValidationIfAppropriate(client);      

        return Optional.<YouViewClient>of(new HttpYouViewClient(
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
