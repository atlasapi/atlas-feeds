package org.atlasapi.feeds.youview;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.genres.GenreMappings;
import org.atlasapi.feeds.youview.ids.IdParsers;
import org.atlasapi.feeds.youview.ids.PublisherIdUtilities;
import org.atlasapi.feeds.youview.images.ImageConfigurations;
import org.atlasapi.feeds.youview.transactions.MongoTransactionStore;
import org.atlasapi.feeds.youview.transactions.TransactionStore;
import org.atlasapi.feeds.youview.www.YouViewFeedController;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.security.UsernameAndPassword;

@Configuration
public class YouViewFeedsWebModule {
    
    private static final String CONFIG_PREFIX = "youview.upload.";

    private static final Map<String, Publisher> PUBLISHER_MAPPING = ImmutableMap.of(
            "lovefilm", Publisher.LOVEFILM,
            "unbox", Publisher.AMAZON_UNBOX,
            "nitro", Publisher.BBC_NITRO
    ); 
    
    private @Value("${youview.upload.validation}") String performValidation;
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    
    @Bean
    public YouViewUploadController uploadController() {
        return new YouViewUploadController(contentFinder, contentResolver, youViewUploadClient());
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
    public YouViewRemoteClient youViewUploadClient() {
        return new YouViewRemoteClient(feedGenerator(), configFactory(), transactionStore());
    }
    
    private YouViewPerPublisherFactory configFactory() {
        YouViewPerPublisherFactory.Builder factory = YouViewPerPublisherFactory.builder();
        for (UploadPublisherConfiguration config : parseConfig()) {
            factory = factory.withPublisher(
                    config.publisher(), 
                    PublisherIdUtilities.idUtilFor(config.publisher(), config.url()), 
                    ImageConfigurations.imageConfigFor(config.publisher()),
                    IdParsers.parserFor(config.publisher()), 
                    GenreMappings.mappingFor(config.publisher()), 
                    httpClient(config.credentials().username(), config.credentials().password())
            );
        }
        return factory.build();
    }
    
    @Bean
    public TransactionStore transactionStore() {
        return new MongoTransactionStore(mongo);
    }
    
    @Bean
    public Set<UploadPublisherConfiguration> parseConfig() {
        ImmutableSet.Builder<UploadPublisherConfiguration> config = ImmutableSet.builder();
        for (Entry<String, Publisher> publisher : PUBLISHER_MAPPING.entrySet()) {
            String publisherPrefix = CONFIG_PREFIX + publisher.getKey();
            boolean isEnabled = Boolean.parseBoolean(Configurer.get(publisherPrefix + ".enabled").get());
            if (isEnabled) {
                config.add(new UploadPublisherConfiguration(publisher.getValue(), parseUrl(publisherPrefix), parseCredentials(publisherPrefix), parseChunkSize(publisherPrefix)));
            }
        }
        return config.build();
    }

    private String parseUrl(String publisherPrefix) {
        return Configurer.get(publisherPrefix + ".url").get();
    }
    
    private UsernameAndPassword parseCredentials(String publisherPrefix) {
        return new UsernameAndPassword(
                Configurer.get(publisherPrefix + ".username").get(), 
                Configurer.get(publisherPrefix + ".password").get()
        );
    }
    
    private int parseChunkSize(String publisherPrefix) {
        return Configurer.get(publisherPrefix + ".chunkSize").toInt();
    }

    private SimpleHttpClient httpClient(String username, String password) {
        return new SimpleHttpClientBuilder()
            .withHeader("Content-Type", "text/xml")
            .withSocketTimeout(1, TimeUnit.MINUTES)
            .withPreemptiveBasicAuth(new UsernameAndPassword(username, password))
            .build();
    }
}
