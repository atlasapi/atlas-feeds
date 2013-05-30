package org.atlasapi.feeds.youview;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
@Import(YouViewFeedsWebModule.class)
public class YouViewUploadModule {
    
    private final static RepetitionRule DELTA_UPLOAD = RepetitionRules.every(Duration.standardHours(12)).withOffset(Duration.standardHours(10));
    private final static RepetitionRule BOOTSTRAP_UPLOAD = RepetitionRules.NEVER;

    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired TvAnytimeGenerator generator;
    private @Autowired YouViewUploader uploader;
    private @Autowired YouViewDeleter deleter;
    
    private @Value("${youview.upload.chunkSize}") int chunkSize;
    
    @PostConstruct
    public void startScheduledTasks() {
        scheduler.schedule(deltaUploader().withName("YouView Lovefilm TVAnytime Delta Upload"), DELTA_UPLOAD);
        scheduler.schedule(bootstrapUploader().withName("YouView Lovefilm TVAnytime Bootstrap Upload"), BOOTSTRAP_UPLOAD);
    }

    @Bean
    public YouViewUploadTask deltaUploader() {
        return new YouViewUploadTask(uploader, deleter, chunkSize, contentFinder, store(), false);
    }
    
    @Bean
    public YouViewUploadTask bootstrapUploader() {
        return new YouViewUploadTask(uploader, deleter, chunkSize, contentFinder, store(), true);
    }

    public @Bean YouViewLastUpdatedStore store() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
}
