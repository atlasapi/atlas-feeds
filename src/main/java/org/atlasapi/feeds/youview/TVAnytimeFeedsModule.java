package org.atlasapi.feeds.youview;

import java.util.Map;

import org.atlasapi.feeds.tvanytime.JaxbTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.PublisherSpecificTVAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmTvAnytimeElementCreator;
import org.atlasapi.feeds.youview.nitro.NitroTvAnytimeElementCreator;
import org.atlasapi.feeds.youview.statistics.FeedStatistics;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsResolver;
import org.atlasapi.feeds.youview.statistics.MockDataFeedStatisticsResolver;
import org.atlasapi.feeds.youview.tasks.persistence.IdSettingTaskStore;
import org.atlasapi.feeds.youview.tasks.persistence.MongoTaskStore;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.unbox.UnboxTvAnytimeElementCreator;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.ids.MongoSequentialIdGenerator;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;

@Configuration
@Import( { NitroTVAnytimeModule.class, LoveFilmTVAnytimeModule.class, UnboxTVAnytimeModule.class } )
public class TVAnytimeFeedsModule {
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired NitroTvAnytimeElementCreator nitroElementCreator;
    private @Autowired LoveFilmTvAnytimeElementCreator loveFilmElementCreator;
    private @Autowired UnboxTvAnytimeElementCreator unboxElementCreator;
    
    @Bean
    public FeedStatisticsResolver feedStatsResolver() {
        FeedStatistics mockedStats = new FeedStatistics(Publisher.BBC_NITRO, 123, Duration.standardMinutes(37), DateTime.now().minusMonths(1));
        return new MockDataFeedStatisticsResolver(mockedStats);
    }
    
    @Bean 
    public TvAnytimeGenerator feedGenerator() {
        Map<Publisher, TvAnytimeGenerator> generatorMapping = ImmutableMap.<Publisher, TvAnytimeGenerator>builder()
                .put(Publisher.BBC_NITRO, new JaxbTvAnytimeGenerator(nitroElementCreator))
                .put(Publisher.LOVEFILM, new JaxbTvAnytimeGenerator(loveFilmElementCreator))
                .put(Publisher.AMAZON_UNBOX, new JaxbTvAnytimeGenerator(unboxElementCreator))
                .build();
        
        return new PublisherSpecificTVAnytimeGenerator(generatorMapping);
    }

    @Bean
    public TaskStore taskStore() {
        return new IdSettingTaskStore(new MongoTaskStore(mongo), new MongoSequentialIdGenerator(mongo, "tasks"));
    }
}
