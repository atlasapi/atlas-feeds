package org.atlasapi.feeds.youview;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.persistence.IdSettingTaskStore;
import org.atlasapi.feeds.tasks.persistence.MongoTaskStore;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.JaxbTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.PublisherSpecificTVAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmBroadcastEventGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmChannelGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmGroupInformationGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmMasterbrandGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmProgramInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastEventGenerator;
import org.atlasapi.feeds.youview.nitro.NitroChannelInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroGroupInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroMasterbrandInfoGenerator;
import org.atlasapi.feeds.youview.nitro.NitroOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroProgramInformationGenerator;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsResolver;
import org.atlasapi.feeds.youview.statistics.MongoFeedStatisticsStore;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastEventGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxChannelGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxGroupInformationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxMasterbrandGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxProgramInformationGenerator;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.ids.MongoSequentialIdGenerator;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;

@Configuration
@Import( { NitroTVAnytimeModule.class, LoveFilmTVAnytimeModule.class, UnboxTVAnytimeModule.class } )
public class TVAnytimeFeedsModule {
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired ContentResolver contentResolver;

    private @Autowired LoveFilmProgramInformationGenerator loveFilmProgInfoGenerator;
    private @Autowired LoveFilmGroupInformationGenerator loveFilmGroupInfoGenerator;
    private @Autowired LoveFilmOnDemandLocationGenerator loveFilmOnDemandGenerator;
    private @Autowired LoveFilmBroadcastEventGenerator loveFilmBroadcastGenerator;
    private @Autowired LoveFilmChannelGenerator loveFilmChannelGenerator;
    private @Autowired LoveFilmMasterbrandGenerator loveFilmMasterbrandGenerator;

    private @Autowired UnboxProgramInformationGenerator unboxProgInfoGenerator;
    private @Autowired UnboxGroupInformationGenerator unboxGroupInfoGenerator;
    private @Autowired UnboxOnDemandLocationGenerator unboxOnDemandGenerator;
    private @Autowired UnboxBroadcastEventGenerator unboxBroadcastGenerator;
    private @Autowired UnboxChannelGenerator unboxChannelGenerator;
    private @Autowired UnboxMasterbrandGenerator unboxMasterbrandGenerator;
    
    private @Autowired NitroProgramInformationGenerator nitroProgInfoGenerator;
    private @Autowired NitroGroupInformationGenerator nitroGroupInfoGenerator;
    private @Autowired NitroOnDemandLocationGenerator nitroOnDemandGenerator;
    private @Autowired NitroBroadcastEventGenerator nitroBroadcastGenerator;
    private @Autowired NitroChannelInformationGenerator nitroChannelInformationGenerator;
    private @Autowired NitroMasterbrandInfoGenerator nitroMasterbrandInfoGenerator;
    
    private Clock clock() {
        return new SystemClock(DateTimeZone.UTC);
    }
    
    @Bean
    public FeedStatisticsResolver feedStatsResolver() {
        return feedStatsStore();
    }
    
    @Bean 
    public FeedStatisticsResolver feedStatsStore() {
        // hardcoded to YOUVIEW as there are no other implementations at present
        return new MongoFeedStatisticsStore(mongo, taskStore(), clock(), DestinationType.YOUVIEW);
    }
    
    @Bean 
    public TvAnytimeGenerator feedGenerator() {
        Map<Publisher, TvAnytimeGenerator> generatorMapping 
                = ImmutableMap.<Publisher, TvAnytimeGenerator>builder()
                .put(Publisher.LOVEFILM, loveFilmTVAGenerator())
                .put(Publisher.AMAZON_UNBOX, unboxTVAGenerator())
                .put(Publisher.BBC_NITRO, nitroTVAGenerator())
                .build();
        
        return new PublisherSpecificTVAnytimeGenerator(generatorMapping);
    }
    
    private TvAnytimeGenerator loveFilmTVAGenerator() {
        return new JaxbTvAnytimeGenerator(new DefaultTvAnytimeElementCreator(
                loveFilmProgInfoGenerator, 
                loveFilmGroupInfoGenerator, 
                loveFilmOnDemandGenerator, 
                loveFilmBroadcastGenerator,
                loveFilmChannelGenerator,
                loveFilmMasterbrandGenerator,
                contentHierarchy()
        ));
    }

    private TvAnytimeGenerator unboxTVAGenerator() {
        return new JaxbTvAnytimeGenerator(new DefaultTvAnytimeElementCreator(
                unboxProgInfoGenerator, 
                unboxGroupInfoGenerator, 
                unboxOnDemandGenerator, 
                unboxBroadcastGenerator,
                unboxChannelGenerator,
                unboxMasterbrandGenerator,
                contentHierarchy()
        ));
    }

    private TvAnytimeGenerator nitroTVAGenerator() {
        return new JaxbTvAnytimeGenerator(new DefaultTvAnytimeElementCreator(
                nitroProgInfoGenerator, 
                nitroGroupInfoGenerator, 
                nitroOnDemandGenerator, 
                nitroBroadcastGenerator,
                nitroChannelInformationGenerator,
                nitroMasterbrandInfoGenerator,
                contentHierarchy()
        ));
    }
    
    @Bean
    public ContentHierarchyExtractor contentHierarchy() {
        return new ContentResolvingContentHierarchyExtractor(contentResolver);
    }

    @Bean
    public TaskStore taskStore() {
        return new IdSettingTaskStore(new MongoTaskStore(mongo), new MongoSequentialIdGenerator(mongo, "tasks"));
    }
}
