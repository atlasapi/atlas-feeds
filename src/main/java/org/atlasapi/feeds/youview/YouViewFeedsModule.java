package org.atlasapi.feeds.youview;

import javax.annotation.PostConstruct;

import org.atlasapi.application.ApplicationStore;
import org.atlasapi.application.MongoApplicationStore;
import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.application.query.IpCheckingApiKeyConfigurationFetcher;
import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.www.YouViewController;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
public class YouViewFeedsModule {
    
//    private final static RepetitionRule DELTA_UPLOAD = RepetitionRules.NEVER;//RepetitionRules.every(Duration.standardHours(12));
//    private final static RepetitionRule BOOTSTRAP_UPLOAD = RepetitionRules.NEVER;//RepetitionRules.every(Duration.standardHours(12));

    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder executor;
    private @Autowired ContentResolver contentResolver;
//    private @Autowired SimpleScheduler scheduler;
//    
//    @PostConstruct
//    public void startScheduledTasks() {
//        scheduler.schedule(deltaUploader().withName("YouView Delta Upload"), DELTA_UPLOAD);
//        scheduler.schedule(bootstrapUploader().withName("YouView Bootstrap Upload"), BOOTSTRAP_UPLOAD);
//    }
//
//    private YouViewDeltaUploader deltaUploader() {
//        return new YouViewDeltaUploader();
//    }
//
//    private YouViewBootstrapUploader bootstrapUploader() {
//        return new YouViewBootstrapUploader();
//    }

    public @Bean YouViewController feedController() {
        return new YouViewController(configFetcher(), feedGenerator(), executor);
    }
    
    public @Bean ApplicationConfigurationFetcher configFetcher(){
        return new IpCheckingApiKeyConfigurationFetcher(applicationStore());
    }
    
    public @Bean ApplicationStore applicationStore(){
        return new MongoApplicationStore(mongo);
    }

    private TvAnytimeGenerator feedGenerator() {
        return new DefaultTvAnytimeGenerator(
            new LovefilmProgramInformationGenerator(), 
            new LovefilmGroupInformationGenerator(), 
            new LovefilmOnDemandLocationGenerator(), 
            new LovefilmServiceInformationGenerator(), 
            new LovefilmInstantServiceInformationGenerator(), 
            contentResolver
        );
    }
}
