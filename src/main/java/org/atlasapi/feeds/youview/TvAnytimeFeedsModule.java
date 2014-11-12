package org.atlasapi.feeds.youview;

import java.util.Map;

import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.feeds.tvanytime.JaxbTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.PublisherSpecificTVAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.genres.GenreMapping;
import org.atlasapi.feeds.youview.nitro.NitroTvAnytimeElementCreator;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastEventGenerator;
import org.atlasapi.feeds.youview.nitro.NitroGenreMapping;
import org.atlasapi.feeds.youview.nitro.NitroGroupInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroProgramInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroServiceMapping;
import org.atlasapi.feeds.youview.services.ServiceMapping;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;

@Configuration
public class TvAnytimeFeedsModule {
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired ChannelResolver channelResolver;
    
    @Bean
    public FeedStatisticsResolver feedStatsResolver() {
        FeedStatistics mockedStats = new FeedStatistics(Publisher.BBC_NITRO, 123, Duration.standardMinutes(37), DateTime.now().minusMonths(1));
        return new MockDataFeedStatisticsResolver(mockedStats);
    }
    
    // TODO add LF and Unbox instantiation
    // The construction of this is kinda hideous
    @Bean 
    public TvAnytimeGenerator feedGenerator() {
        Map<Publisher, TvAnytimeGenerator> generatorMapping = ImmutableMap.<Publisher, TvAnytimeGenerator>builder()
                .put(Publisher.BBC_NITRO, new JaxbTvAnytimeGenerator(nitroElementCreator()))
                .build();
        
        return new PublisherSpecificTVAnytimeGenerator(generatorMapping);
    }
    
    private TvAnytimeElementCreator nitroElementCreator() {
        IdGenerator nitroIdGenerator = new NitroIdGenerator();
        GenreMapping genreMapping = genreMappingFor(Publisher.BBC_NITRO);
        ServiceMapping serviceMapping = serviceMappingFor(Publisher.BBC_NITRO);
        
        return new NitroTvAnytimeElementCreator(
                new NitroProgramInformationGenerator(nitroIdGenerator, elementFactory()), 
                new NitroGroupInformationGenerator(nitroIdGenerator, genreMapping), 
                new NitroOnDemandLocationGenerator(nitroIdGenerator, elementFactory()), 
                new NitroBroadcastEventGenerator(nitroIdGenerator, elementFactory(), serviceMapping, channelResolver),
                contentHierarchy(), 
                new UriBasedContentPermit()
        );
    }

    // TODO implement this
    private GenreMapping genreMappingFor(Publisher publisher) {
        if (Publisher.BBC_NITRO.equals(publisher)) {
            return new NitroGenreMapping();
        }
        return null;
    }

    // TODO implement this
    private ServiceMapping serviceMappingFor(Publisher publisher) {
        if (Publisher.BBC_NITRO.equals(publisher)) {
            return new NitroServiceMapping();
        }
        return null;
    }

    @Bean
    public TvAnytimeElementFactory elementFactory() {
        return new TvAnytimeElementFactory();
    }
    
    private ContentHierarchyExtractor contentHierarchy() {
        return new ContentResolvingContentHierarchyExtractor(contentResolver);
    }

//    private YouViewPerPublisherFactory configFactory() {
//        YouViewPerPublisherFactory.Builder factory = YouViewPerPublisherFactory.builder();
//        for (UploadPublisherConfiguration config : parseConfig().getConfigs()) {
//            factory = factory.withPublisher(
//                    config.publisher(), 
//                    PublisherIdUtilities.idUtilFor(config.publisher(), config.url()), 
//                    ImageConfigurations.imageConfigFor(config.publisher()),
//                    IdParsers.parserFor(config.publisher()), 
//                    GenreMappings.mappingFor(config.publisher()), 
//                    httpClient(config.credentials().username(), config.credentials().password())
//            );
//        }
//        return factory.build();
//    }
    
    @Bean
    public TransactionStore transactionStore() {
        return new MongoTransactionStore(mongo);
    }
}
