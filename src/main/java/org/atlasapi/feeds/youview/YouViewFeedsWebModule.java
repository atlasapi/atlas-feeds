package org.atlasapi.feeds.youview;

import org.atlasapi.application.query.ApplicationSourcesFetcher;
import org.atlasapi.application.query.ApiKeyConfigurationFetcher;
import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.www.YouViewController;
import org.atlasapi.persistence.application.ApplicationStore;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YouViewFeedsWebModule {
    
    private @Value("${youview.upload.validation}") String validation;
    
    private @Autowired @Qualifier(value = "deerApplicationsStore") ApplicationStore deerApplicationsStore;

    private @Autowired LastUpdatedContentFinder executor;
    private @Autowired ContentResolver contentResolver;
    
    public @Bean YouViewController feedController() {
        return new YouViewController(configFetcher(), feedGenerator(), executor);
    }
    
    public @Bean ApplicationSourcesFetcher configFetcher(){
        return new ApiKeyConfigurationFetcher(deerApplicationsStore);
    }

    public @Bean TvAnytimeGenerator feedGenerator() {
        return new DefaultTvAnytimeGenerator(
            new LoveFilmProgramInformationGenerator(), 
            new LoveFilmGroupInformationGenerator(genreMapping()), 
            new LoveFilmOnDemandLocationGenerator(), 
            contentResolver,
            Boolean.parseBoolean(validation)
        );
    }

    public @Bean YouViewGenreMapping genreMapping() {
        return new YouViewGenreMapping();
    }
}
