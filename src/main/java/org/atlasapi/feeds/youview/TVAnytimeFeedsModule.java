package org.atlasapi.feeds.youview;

import java.util.Map;

import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.persistence.IdSettingTaskStore;
import org.atlasapi.feeds.tasks.persistence.MongoTaskStore;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.JaxbTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.PublisherSpecificTVAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastEventGenerator;
import org.atlasapi.feeds.youview.nitro.NitroChannelInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroGroupInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroMasterbrandInfoGenerator;
import org.atlasapi.feeds.youview.nitro.NitroOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroProgramInformationGenerator;
import org.atlasapi.feeds.youview.statistics.FeedStatisticsResolver;
import org.atlasapi.feeds.youview.statistics.MongoFeedStatisticsStore;
import org.atlasapi.feeds.youview.unbox.AmazonBroadcastEventGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonChannelGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonGroupInformationGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonMasterbrandGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonProgramInformationGenerator;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.ids.MongoSequentialIdGenerator;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import( { NitroTVAnytimeModule.class, AmazonTVAnytimeModule.class } )
public class TVAnytimeFeedsModule {
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired ContentResolver contentResolver;

    private @Autowired AmazonProgramInformationGenerator unboxProgInfoGenerator;
    private @Autowired AmazonGroupInformationGenerator unboxGroupInfoGenerator;
    private @Autowired AmazonOnDemandLocationGenerator unboxOnDemandGenerator;
    private @Autowired AmazonBroadcastEventGenerator unboxBroadcastGenerator;
    private @Autowired AmazonChannelGenerator amazonChannelGenerator;
    private @Autowired AmazonMasterbrandGenerator amazonMasterbrandGenerator;

    private @Autowired org.atlasapi.feeds.youview.amazon.AmazonProgramInformationGenerator newAmazonProgInfoGenerator;
    private @Autowired org.atlasapi.feeds.youview.amazon.AmazonGroupInformationGenerator newAmazonGroupInfoGenerator;
    private @Autowired org.atlasapi.feeds.youview.amazon.AmazonOnDemandLocationGenerator newAmazonOnDemandGenerator;
    private @Autowired org.atlasapi.feeds.youview.amazon.AmazonBroadcastEventGenerator newAmazonBroadcastGenerator;
    private @Autowired org.atlasapi.feeds.youview.amazon.AmazonChannelGenerator newAmazonChannelGenerator;
    private @Autowired org.atlasapi.feeds.youview.amazon.AmazonMasterbrandGenerator newAmazonMasterbrandGenerator;
    
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
    public ContentHierarchyExpanderFactory contentHierarchyExpanderFactory(){
        return new ContentHierarchyExpanderFactory();
    }

    @Bean
    public FeedStatisticsResolver feedStatsResolver() {
        return feedStatsStore();
    }
    
    @Bean 
    public FeedStatisticsResolver feedStatsStore() {
        // hardcoded to YOUVIEW as there are no other implementations at present
        return MongoFeedStatisticsStore.builder()
                .withMongoDatabase(mongo)
                .withTaskStore(taskStore())
                .withClock(clock())
                .withDestinationType(DestinationType.YOUVIEW)
                .build();
    }
    
    @Bean 
    public TvAnytimeGenerator feedGenerator() {
        Map<Publisher, TvAnytimeGenerator> generatorMapping 
                = ImmutableMap.<Publisher, TvAnytimeGenerator>builder()
                .put(Publisher.AMAZON_UNBOX, unboxTVAGenerator())
                .put(Publisher.AMAZON_V3, amazonTVAGenerator())
                .put(Publisher.BBC_NITRO, nitroTVAGenerator())
                .build();
        
        return new PublisherSpecificTVAnytimeGenerator(generatorMapping);
    }

    private TvAnytimeGenerator amazonTVAGenerator() {
        return new JaxbTvAnytimeGenerator(new DefaultTvAnytimeElementCreator(
                newAmazonProgInfoGenerator,
                newAmazonGroupInfoGenerator,
                newAmazonOnDemandGenerator,
                newAmazonBroadcastGenerator,
                newAmazonChannelGenerator,
                newAmazonMasterbrandGenerator,
                contentHierarchy()
        ));
    }

    private TvAnytimeGenerator unboxTVAGenerator() {
        return new JaxbTvAnytimeGenerator(new DefaultTvAnytimeElementCreator(
                unboxProgInfoGenerator, 
                unboxGroupInfoGenerator, 
                unboxOnDemandGenerator, 
                unboxBroadcastGenerator,
                amazonChannelGenerator,
                amazonMasterbrandGenerator,
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
