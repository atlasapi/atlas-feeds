package org.atlasapi.feeds.youview;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.entity.Publisher;
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
    private static final RepetitionRule UNBOX_DELTA_UPLOAD = RepetitionRules.NEVER;//RepetitionRules.every(Duration.standardHours(12)).withOffset(Duration.standardHours(12));

    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired TvAnytimeGenerator generator;
    private @Autowired YouViewRemoteClient youViewClient;
    
    private @Value("${youview.upload.lovefilm.chunkSize}") int loveFilmChunkSize;
    private @Value("${youview.upload.unbox.chunkSize}") int unboxChunkSize;
    
    @PostConstruct
    public void startScheduledTasks() {
        scheduler.schedule(uploadTask(Publisher.LOVEFILM, loveFilmChunkSize, false).withName("YouView Lovefilm TVAnytime Delta Upload"), DELTA_UPLOAD);
        scheduler.schedule(uploadTask(Publisher.LOVEFILM, loveFilmChunkSize, true).withName("YouView Lovefilm TVAnytime Bootstrap Upload"), BOOTSTRAP_UPLOAD);
        scheduler.schedule(uploadTask(Publisher.AMAZON_UNBOX, unboxChunkSize, false).withName("YouView Amazon Unbox TVAnytime Delta Upload"), UNBOX_DELTA_UPLOAD);
        scheduler.schedule(uploadTask(Publisher.AMAZON_UNBOX, unboxChunkSize, true).withName("YouView Amazon Unbox TVAnytime Bootstrap Upload"), BOOTSTRAP_UPLOAD);
    }

    private YouViewUploadTask uploadTask(Publisher publisher, int chunkSize, boolean isBootstrap) {
        return new YouViewUploadTask(youViewClient, chunkSize, contentFinder, store(), publisher, isBootstrap);
    }

    public @Bean YouViewLastUpdatedStore store() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
}
