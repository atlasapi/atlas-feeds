package org.atlasapi.feeds.youview;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.feeds.youview.upload.HttpYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.PublisherDelegatingYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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
import com.metabroadcast.common.time.SystemClock;

@Configuration
@Import(TvAnytimeFeedsModule.class)
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
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired TvAnytimeGenerator generator;
    private @Autowired HttpYouViewRemoteClient youViewClient;
    private @Autowired TransactionStore transactionStore;
    private @Autowired BbcServiceIdResolver bbcServiceIdResolver;
    
    private @Value("${youview.upload.validation}") String performValidation;

    @PostConstruct
    public void startScheduledTasks() {
//        for (UploadPublisherConfiguration config : uploadConfig.getConfigs()) {
        // TODO this duplicates the loop instantiating youview clients - fix this
        for (Entry<String, Publisher> publisherEntry : PUBLISHER_MAPPING.entrySet()) {
            String publisherPrefix = CONFIG_PREFIX + publisherEntry.getKey();
            if (isEnabled(publisherPrefix)) {
                scheduler.schedule(scheduleTask(publisherEntry.getValue(), true, "Bootstrap"), BOOTSTRAP_UPLOAD);
                scheduler.schedule(scheduleTask(publisherEntry.getValue(), false, "Delta"), DELTA_UPLOAD);
                scheduler.schedule(remoteCheckTask(publisherEntry.getValue()).withName("YouView Transaction Status Check: " + publisherEntry.getValue().name()), REMOTE_CHECK_UPLOAD);
            }
        }
    }
    
    private ScheduledTask remoteCheckTask(Publisher publisher) {
        return new YouViewRemoteCheckTask(transactionStore, publisher, youViewClient);
    }

    // TODO this should only work for those publishers whose feeds are enabled
    @Bean
    public YouViewUploadController uploadController() {
        return new YouViewUploadController(contentFinder, contentResolver, youViewUploadClient(), transactionStore);
    }

    private ScheduledTask scheduleTask(Publisher publisher, boolean isBootstrap, String taskKey) {
        return uploadTask(publisher, isBootstrap).withName(String.format(TASK_NAME_PATTERN, taskKey, publisher.title()));
    }

    private YouViewUploadTask uploadTask(Publisher publisher, boolean isBootstrap) {
        return new YouViewUploadTask(youViewUploadClient(), contentFinder, store(), publisher, isBootstrap, transactionStore);
    }

    public @Bean YouViewLastUpdatedStore store() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
    
    @Bean
    public YouViewRemoteClient youViewUploadClient() {
        ImmutableMap.Builder<Publisher, YouViewRemoteClient> clients = ImmutableMap.builder();
        for (Entry<String, Publisher> publisherEntry : PUBLISHER_MAPPING.entrySet()) {
            String publisherPrefix = CONFIG_PREFIX + publisherEntry.getKey();
            if (isEnabled(publisherPrefix)) {
                UsernameAndPassword credentials = parseCredentials(publisherPrefix);
                YouViewRemoteClient client = new HttpYouViewRemoteClient(
                        generator, 
                        httpClient(credentials.username(), credentials.password()),
                        parseUrl(publisherPrefix),
                        // TODO this is suboptimal - this should use the same generator as the tvanytimefeedsmodule
                        // this is also hardcoded to the nitro generator
                        new NitroIdGenerator(bbcServiceIdResolver),
                        new SystemClock(), 
                        Boolean.parseBoolean(performValidation)
                );
                clients.put(Publisher.BBC_NITRO, client);
            }
        }
        
        return new PublisherDelegatingYouViewRemoteClient(clients.build());
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
