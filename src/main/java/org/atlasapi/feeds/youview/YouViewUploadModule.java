package org.atlasapi.feeds.youview;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.security.UsernameAndPassword;

@Configuration
@Import(YouViewFeedsWebModule.class)
public class YouViewUploadModule {
    
    private final static RepetitionRule DELTA_UPLOAD = RepetitionRules.NEVER;//RepetitionRules.every(Duration.standardHours(12));
    private final static RepetitionRule BOOTSTRAP_UPLOAD = RepetitionRules.NEVER;//RepetitionRules.every(Duration.standardHours(12));

    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired TvAnytimeGenerator generator;
    
    private @Value("${youview.upload.validation}") String validation;
    private @Value("${youview.upload.enabled}") String enabled;
    private @Value("${youview.upload.url}") String url;
    private @Value("${youview.upload.username}") String username;
    private @Value("${youview.upload.password}") String password;
    private @Value("${youview.upload.chunkSize}") int chunkSize;
    private @Value("${youview.upload.timeout}") int timeout;
    
    @PostConstruct
    public void startScheduledTasks() {
        scheduler.schedule(deltaUploader().withName("YouView Lovefilm TVAnytime Delta Upload"), DELTA_UPLOAD);
        scheduler.schedule(bootstrapUploader().withName("YouView Lovefilm TVAnytime Bootstrap Upload"), BOOTSTRAP_UPLOAD);
    }

    @Bean
    public YouViewUploadTask deltaUploader() {
        return new YouViewUploadTask(uploader(), deleter(), chunkSize, contentFinder, store(), false);
    }
    
    @Bean
    public YouViewUploadTask bootstrapUploader() {
        return new YouViewUploadTask(uploader(), deleter(), chunkSize, contentFinder, store(), true);
    }

    @Bean
    public YouViewDeleter deleter() {
        return new YouViewDeleter(url, httpClient());
    }

    @Bean
    public YouViewUploader uploader() {
        return new YouViewUploader(url, generator, httpClient());
    }

    public @Bean YouViewLastUpdatedStore store() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
    
    private SimpleHttpClient httpClient() {
        return new SimpleHttpClientBuilder()
            .withHeader("Content-Type", "text/xml")
            .withSocketTimeout(1, TimeUnit.MINUTES)
            .withPreemptiveBasicAuth(new UsernameAndPassword(username, password))
            .build();
    }
}
