package org.atlasapi.feeds.youview;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.security.UsernameAndPassword;

@Configuration
public class YouViewUploadModule {
    
    private final static RepetitionRule DELTA_UPLOAD = RepetitionRules.NEVER;//RepetitionRules.every(Duration.standardHours(12));
    private final static RepetitionRule BOOTSTRAP_UPLOAD = RepetitionRules.NEVER;//RepetitionRules.every(Duration.standardHours(12));

    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder executor;
    private @Autowired ContentResolver contentResolver;
    private @Autowired SimpleScheduler scheduler;
    
    private @Value("${youview.upload.validation}") String validation;
    private @Value("${youview.upload.enabled}") String enabled;
    private @Value("${youview.upload.url}") String url;
    private @Value("${youview.upload.username}") String username;
    private @Value("${youview.upload.password}") String password;
    private @Value("${youview.upload.genresFile}") String genreFilePath;
    
    @PostConstruct
    public void startScheduledTasks() {
        if(Boolean.parseBoolean(enabled)) {
            scheduler.schedule(deltaUploader().withName("YouView Delta Upload"), DELTA_UPLOAD);
            scheduler.schedule(bootstrapUploader().withName("YouView Bootstrap Upload"), BOOTSTRAP_UPLOAD);
        }
    }

    public @Bean YouViewDeltaUploader deltaUploader() {
        return new YouViewDeltaUploader(url, executor, feedGenerator(), new UsernameAndPassword(username, password), store());
    }

    public @Bean YouViewBootstrapUploader bootstrapUploader() {
        return new YouViewBootstrapUploader(url, executor, feedGenerator(), new UsernameAndPassword(username, password), store());
    }
    
    public @Bean YouViewLastUpdatedStore store() {
        return new MongoYouViewLastUpdatedStore(mongo);
    }

    public @Bean YouViewGenreMapping genreMapping() {
        return new YouViewGenreMapping(genreFilePath);
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
}
