package org.atlasapi.feeds.youview;

import java.util.concurrent.TimeUnit;

import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.www.YouViewFeedController;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.entity.Publisher;
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
    
    private @Value("${youview.upload.validation}") String performValidation;
    private @Value("${youview.upload.lovefilm.url}") String loveFilmUrl;
    private @Value("${youview.upload.lovefilm.username}") String loveFilmUsername;
    private @Value("${youview.upload.lovefilm.password}") String loveFilmPassword;
    private @Value("${youview.upload.unbox.url}") String unboxUrl;
    private @Value("${youview.upload.unbox.username}") String unboxUsername;
    private @Value("${youview.upload.unbox.password}") String unboxPassword;
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    
    @Bean
    public YouViewUploadController uploadController() {
        return new YouViewUploadController(contentFinder, contentResolver, loveFilmClient());
    }
    
    @Bean
    public YouViewFeedController feedController() {
        return new YouViewFeedController(feedGenerator(), contentFinder, contentResolver);
    }
    
    @Bean 
    public TvAnytimeGenerator feedGenerator() {
        return new DefaultTvAnytimeGenerator(
            new DefaultProgramInformationGenerator(configFactory()), 
            new DefaultGroupInformationGenerator(configFactory()), 
            new DefaultOnDemandLocationGenerator(configFactory()), 
            contentResolver,
            Boolean.parseBoolean(performValidation)
        );
    }
    
    @Bean
    public YouViewRemoteClient loveFilmClient() {
        return new YouViewRemoteClient(feedGenerator(), configFactory());
    }
    
    private YouViewPerPublisherFactory configFactory() {
        return YouViewPerPublisherFactory.builder()
                .withPublisher(
                        Publisher.LOVEFILM, 
                        new LoveFilmPublisherConfiguration(loveFilmUrl), 
                        new LoveFilmIdParser(), 
                        new LoveFilmGenreMap(), 
                        httpClient(loveFilmUsername, loveFilmPassword))
                .withPublisher(
                        Publisher.AMAZON_UNBOX, 
                        new UnboxPublisherConfiguration(unboxUrl), 
                        new UnboxIdParser(), 
                        new UnboxGenreMap(), 
                        httpClient(unboxUsername, unboxPassword))
                .build();
    }
    
    private SimpleHttpClient httpClient(String username, String password) {
        return new SimpleHttpClientBuilder()
            .withHeader("Content-Type", "text/xml")
            .withSocketTimeout(1, TimeUnit.MINUTES)
            .withPreemptiveBasicAuth(new UsernameAndPassword(username, password))
            .build();
    }
}
