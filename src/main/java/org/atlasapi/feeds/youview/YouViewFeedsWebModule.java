package org.atlasapi.feeds.youview;

import java.util.concurrent.TimeUnit;

import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.www.YouViewController;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.security.UsernameAndPassword;

@Configuration
public class YouViewFeedsWebModule {
    
    private @Value("${youview.upload.validation}") String validation;
    private @Value("${youview.upload.url}") String url;
    private @Value("${youview.upload.username}") String username;
    private @Value("${youview.upload.password}") String password;
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder executor;
    private @Autowired ContentResolver contentResolver;
    
    public @Bean YouViewController feedController() {
        return new YouViewController(feedGenerator(), executor, contentResolver, uploader(), deleter());
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

    @Bean
    public YouViewUploader uploader() {
        return new YouViewUploader(url, feedGenerator(), httpClient());
    }

    @Bean
    public YouViewDeleter deleter() {
        return new YouViewDeleter(url, httpClient());
    }
    
    private SimpleHttpClient httpClient() {
        return new SimpleHttpClientBuilder()
            .withHeader("Content-Type", "text/xml")
            .withSocketTimeout(1, TimeUnit.MINUTES)
            .withPreemptiveBasicAuth(new UsernameAndPassword(username, password))
            .build();
    }
}
