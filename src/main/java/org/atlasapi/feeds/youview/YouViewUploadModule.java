package org.atlasapi.feeds.youview;

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
    private @Autowired LastUpdatedContentFinder executor;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired TvAnytimeGenerator generator;
    
    private @Value("${youview.upload.validation}") String validation;
    private @Value("${youview.upload.enabled}") String enabled;
    private @Value("${youview.upload.url}") String url;
    private @Value("${youview.upload.username}") String username;
    private @Value("${youview.upload.password}") String password;
    private @Value("${youview.upload.chunkSize}") String chunkSize;
    
    @PostConstruct
    public void startScheduledTasks() {
        if(Boolean.parseBoolean(enabled)) {
            scheduler.schedule(deltaUploader().withName("YouView Delta Upload"), DELTA_UPLOAD);
            scheduler.schedule(bootstrapUploader().withName("YouView Bootstrap Upload"), BOOTSTRAP_UPLOAD);
        }
    }

    public @Bean YouViewUploader deltaUploader() {
        return new YouViewUploader(url, executor, generator, new UsernameAndPassword(username, password), store(), Integer.parseInt(chunkSize), false);
    }

    public @Bean YouViewUploader bootstrapUploader() {
        return new YouViewUploader(url, executor, generator, new UsernameAndPassword(username, password), store(), Integer.parseInt(chunkSize), true);
    }
    
    public @Bean YouViewLastUpdatedStore store() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }
}
