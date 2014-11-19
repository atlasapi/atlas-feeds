package org.atlasapi.feeds.youview;

import java.util.Map;

import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.feeds.tvanytime.JaxbTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.PublisherSpecificTVAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.genres.GenreMapping;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.ChannelResolvingBbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastEventGenerator;
import org.atlasapi.feeds.youview.nitro.NitroGenreMapping;
import org.atlasapi.feeds.youview.nitro.NitroGroupInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroProgramInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroServiceMapping;
import org.atlasapi.feeds.youview.nitro.NitroTvAnytimeElementCreator;
import org.atlasapi.feeds.youview.services.ServiceMapping;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsResolver;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsStore;
import org.atlasapi.feeds.youview.statistics.MongoFeedStatisticsStore;
import org.atlasapi.feeds.youview.transactions.persistence.MongoTransactionStore;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.SystemClock;

@Configuration
public class TVAnytimeFeedsModule {
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired LastUpdatedContentFinder contentFinder;
    private @Autowired ContentResolver contentResolver;
    private @Autowired ChannelResolver channelResolver;
    
    @Bean
    public FeedStatisticsResolver feedStatsResolver() {
        return feedStatsStore();
    }
    
    @Bean 
    public FeedStatisticsStore feedStatsStore() {
        return new MongoFeedStatisticsStore(mongo, new SystemClock());
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
        IdGenerator nitroIdGenerator = new NitroIdGenerator(bbcServiceIdResolver(), hashFunction());
        GenreMapping genreMapping = genreMappingFor(Publisher.BBC_NITRO);
        ServiceMapping serviceMapping = serviceMappingFor(Publisher.BBC_NITRO);
        
        return new NitroTvAnytimeElementCreator(
                new NitroProgramInformationGenerator(nitroIdGenerator, elementFactory()), 
                new NitroGroupInformationGenerator(nitroIdGenerator, genreMapping, bbcServiceIdResolver()), 
                new NitroOnDemandLocationGenerator(nitroIdGenerator, elementFactory()), 
                new NitroBroadcastEventGenerator(nitroIdGenerator, elementFactory(), serviceMapping, bbcServiceIdResolver()),
                contentHierarchy(), 
                new UriBasedContentPermit()
        );
    }
    
    private HashFunction hashFunction() {
        return Hashing.md5();
    }

    @Bean
    public BbcServiceIdResolver bbcServiceIdResolver() {
        return new ChannelResolvingBbcServiceIdResolver(channelResolver);
    }

    // TODO pull out other service mappings, create delegating genremapping
    private GenreMapping genreMappingFor(Publisher publisher) {
        if (Publisher.BBC_NITRO.equals(publisher)) {
            return new NitroGenreMapping("nitro_genre_mapping.csv");
        }
        return null;
    }

    // TODO implement other service mappings, create delegating servicemapping
    private ServiceMapping serviceMappingFor(Publisher publisher) {
        if (Publisher.BBC_NITRO.equals(publisher)) {
            return new NitroServiceMapping("nitro_service_mapping.csv");
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

    @Bean
    public TransactionStore transactionStore() {
        return new MongoTransactionStore(mongo);
    }
}
