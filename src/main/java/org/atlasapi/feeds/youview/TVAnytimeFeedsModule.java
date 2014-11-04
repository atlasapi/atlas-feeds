package org.atlasapi.feeds.youview;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TVAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.TVAnytimeElementFactory;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.genres.GenreMappings;
import org.atlasapi.feeds.youview.ids.IdParsers;
import org.atlasapi.feeds.youview.ids.PublisherIdUtilities;
import org.atlasapi.feeds.youview.images.ImageConfigurations;
import org.atlasapi.feeds.youview.statistics.FeedStatistics;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsResolver;
import org.atlasapi.feeds.youview.statistics.MockDataFeedStatisticsResolver;
import org.atlasapi.feeds.youview.transactions.persistence.MongoTransactionStore;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.joda.time.Duration;
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
import com.metabroadcast.common.time.SystemClock;

@Configuration
public class TVAnytimeFeedsModule {
    
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
    private @Autowired ChannelResolver channelResolver;
    
    @Bean
    public FeedStatisticsResolver feedStatsResolver() {
        FeedStatistics mockedStats = new FeedStatistics(Publisher.BBC_NITRO, 123, Duration.standardMinutes(37), DateTime.now().minusMonths(1));
        return new MockDataFeedStatisticsResolver(mockedStats);
    }
    
    @Bean 
    public TvAnytimeGenerator feedGenerator() {
        return new DefaultTvAnytimeGenerator(elementCreator());
    }
    
    private TVAnytimeElementCreator elementCreator() {
        return new DefaultTvAnytimeElementCreator(
                new DefaultProgramInformationGenerator(configFactory()), 
                new DefaultGroupInformationGenerator(configFactory()), 
                new DefaultOnDemandLocationGenerator(configFactory()), 
                new DefaultBroadcastEventGenerator(elementFactory(), broadcastIdGenerator(), channelResolver),
                contentHierarchy(), 
                new UriBasedContentPermit()
        );
    }

    private TVAnytimeElementFactory elementFactory() {
        return new TVAnytimeElementFactory();
    }
    
    private BroadcastIdGenerator broadcastIdGenerator() {
        return new UUIDBasedBroadcastIdGenerator();
    }

    private ContentHierarchyExtractor contentHierarchy() {
        return new ContentResolvingContentHierarchyExtractor(contentResolver);
    }

    @Bean
    public YouViewRemoteClient youViewUploadClient() {
        return new YouViewRemoteClient(
                feedGenerator(), 
                configFactory(), 
                transactionStore(), 
                new SystemClock(), 
                Boolean.parseBoolean(performValidation)
        );
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
            boolean isEnabled = isEnabled(publisherPrefix);
            if (isEnabled) {
                config.add(new UploadPublisherConfiguration(
                        publisher.getValue(), 
                        parseUrl(publisherPrefix), 
                        parseCredentials(publisherPrefix), 
                        parseChunkSize(publisherPrefix))
                );
            }
        }
        return config.build();
    }

    private boolean isEnabled(String publisherPrefix) {
        return Boolean.parseBoolean(Configurer.get(publisherPrefix + ".upload.enabled").get());
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
