package org.atlasapi.feeds.youview;

import org.atlasapi.application.ApplicationStore;
import org.atlasapi.application.MongoApplicationStore;
import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.application.query.IpCheckingApiKeyConfigurationFetcher;
import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.www.YouViewController;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;

@Configuration
public class YouViewFeedsWebModule {
    
    private @Value("${youview.upload.validation}") String validation;
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder executor;
    private @Autowired ContentResolver contentResolver;
    
    public @Bean YouViewController feedController() {
        return new YouViewController(configFetcher(), feedGenerator(), executor);
    }
    
    public @Bean ApplicationConfigurationFetcher configFetcher(){
        return new IpCheckingApiKeyConfigurationFetcher(applicationStore());
    }
    
    public @Bean ApplicationStore applicationStore() {
        return new MongoApplicationStore(mongo);
    }

    public @Bean TvAnytimeGenerator feedGenerator() {
        return new DefaultTvAnytimeGenerator(
            new LoveFilmProgramInformationGenerator(), 
            new LoveFilmGroupInformationGenerator(genreMapping()), 
            new LoveFilmOnDemandLocationGenerator(), 
            new LoveFilmServiceInformationGenerator(), 
            new LoveFilmInstantServiceInformationGenerator(), 
            contentResolver,
            Boolean.parseBoolean(validation)
        );
    }

    public @Bean YouViewGenreMapping genreMapping() {
        return new YouViewGenreMapping();
    }
}
