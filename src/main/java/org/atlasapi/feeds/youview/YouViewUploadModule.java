package org.atlasapi.feeds.youview;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsStore;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
@Import(TVAnytimeFeedsModule.class)
public class YouViewUploadModule {
    
    private final static RepetitionRule DELTA_UPLOAD = RepetitionRules.NEVER;//RepetitionRules.every(Duration.standardHours(12)).withOffset(Duration.standardHours(10));
    private final static RepetitionRule BOOTSTRAP_UPLOAD = RepetitionRules.NEVER;
    private final static RepetitionRule REMOTE_CHECK_UPLOAD = RepetitionRules.every(Duration.standardMinutes(15));
    private static final String TASK_NAME_PATTERN = "YouView %s TVAnytime %s Upload";
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired TvAnytimeGenerator generator;
    private @Autowired YouViewRemoteClient youViewClient;
    private @Autowired UploadPublisherConfigurations uploadConfig;
    private @Autowired YouViewRemoteClient youViewRemoteClient;
    private @Autowired TransactionStore transactionStore;
    private @Autowired FeedStatisticsStore feedStatsStore;

    @PostConstruct
    public void startScheduledTasks() {
        for (UploadPublisherConfiguration config : uploadConfig.getConfigs()) {
            scheduler.schedule(scheduleTask(config.publisher(), config.chunkSize(), true, "Bootstrap"), BOOTSTRAP_UPLOAD);
            scheduler.schedule(scheduleTask(config.publisher(), config.chunkSize(), false, "Delta"), DELTA_UPLOAD);
            scheduler.schedule(remoteCheckTask(config.publisher()), REMOTE_CHECK_UPLOAD);
        }
    }
    
    private ScheduledTask remoteCheckTask(Publisher publisher) {
        return new YouViewRemoteCheckTask(transactionStore, publisher, youViewClient);
    }

    // TODO this should only work for those publishers whose feeds are enabled
    @Bean
    public YouViewUploadController uploadController() {
        return new YouViewUploadController(contentFinder, contentResolver, youViewRemoteClient, transactionStore);
    }

    private ScheduledTask scheduleTask(Publisher publisher, int chunkSize, boolean isBootstrap, String taskKey) {
        return uploadTask(publisher, chunkSize, isBootstrap).withName(String.format(TASK_NAME_PATTERN, taskKey, publisher.title()));
    }

    private YouViewUploadTask uploadTask(Publisher publisher, int chunkSize, boolean isBootstrap) {
        return new YouViewUploadTask(youViewClient, chunkSize, contentFinder, store(), publisher, isBootstrap, transactionStore, feedStatsStore);
    }

    public @Bean YouViewLastUpdatedStore store() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
}
