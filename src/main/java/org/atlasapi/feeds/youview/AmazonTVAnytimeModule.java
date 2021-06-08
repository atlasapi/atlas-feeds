package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.unbox.AmazonIdGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonBroadcastEventGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonBroadcastServiceMapping;
import org.atlasapi.feeds.youview.unbox.AmazonChannelGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonGenreMapping;
import org.atlasapi.feeds.youview.unbox.AmazonGroupInformationGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonMasterbrandGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonProgramInformationGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonServiceIdResolver;
import org.atlasapi.media.channel.ChannelResolver;

import com.metabroadcast.common.time.SystemClock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AmazonTVAnytimeModule {

    private @Autowired ChannelResolver channelResolver;
    
    @Bean
    public AmazonProgramInformationGenerator amazonProgInfoGenerator() {
        return new AmazonProgramInformationGenerator(amazonIdGenerator());
    }
    
    @Bean
    public AmazonGroupInformationGenerator amazonGroupInfoGenerator() {
        return new AmazonGroupInformationGenerator(amazonIdGenerator(), amazonGenreMapping());
    }
    
    @Bean
    public AmazonOnDemandLocationGenerator amazonOnDemandGenerator() {
        return new AmazonOnDemandLocationGenerator(amazonIdGenerator());
    }
    
    @Bean
    public AmazonBroadcastEventGenerator amazonBroadcastGenerator() {
        return new AmazonBroadcastEventGenerator();
    }
    @Bean
    public AmazonChannelGenerator amazonChannelGenerator() {
        return new AmazonChannelGenerator();
    }

    @Bean
    public AmazonMasterbrandGenerator amazonMasterbrandGenerator() {
        return new AmazonMasterbrandGenerator();
    }
    @Bean
    public AmazonIdGenerator amazonIdGenerator() {
        return new AmazonIdGenerator();
    }
    
    @Bean
    public AmazonGenreMapping amazonGenreMapping() {
        return new AmazonGenreMapping();
    }
    
    @Bean
    public AmazonBroadcastServiceMapping amazonBroadcastServiceMapping() {
        return new AmazonBroadcastServiceMapping();
    }

    public ServiceIdResolver amazonServiceIdResolver() {
        return new AmazonServiceIdResolver();
    }

    @Bean
    public ContentHierarchyExpander amazonContentHierarchyExpander() {
        return new ContentHierarchyExpanderImpl(versionHierarchyExpander(), broadcastHierarchyExpander(), onDemandHierarchyExpander(), amazonIdGenerator());
    }

    public VersionHierarchyExpander versionHierarchyExpander() {
        return new VersionHierarchyExpander(amazonIdGenerator());
    }

    public OnDemandHierarchyExpander onDemandHierarchyExpander() {
        return new OnDemandHierarchyExpander(amazonIdGenerator());
    }

    public BroadcastHierarchyExpander broadcastHierarchyExpander() {
        return new BroadcastHierarchyExpander(amazonIdGenerator(), amazonBroadcastServiceMapping(), amazonServiceIdResolver() , new SystemClock());
    }

    @Bean
    public org.atlasapi.feeds.youview.amazon.AmazonProgramInformationGenerator newAmazonProgInfoGenerator() {
        return new org.atlasapi.feeds.youview.amazon.AmazonProgramInformationGenerator(newAmazonIdGenerator());
    }

    @Bean
    public org.atlasapi.feeds.youview.amazon.AmazonGroupInformationGenerator newAmazonGroupInfoGenerator() {
        return new org.atlasapi.feeds.youview.amazon.AmazonGroupInformationGenerator(newAmazonIdGenerator(), newAmazonGenreMapping());
    }

    @Bean
    public org.atlasapi.feeds.youview.amazon.AmazonOnDemandLocationGenerator newAmazonOnDemandGenerator() {
        return new org.atlasapi.feeds.youview.amazon.AmazonOnDemandLocationGenerator(newAmazonIdGenerator());
    }

    @Bean
    public org.atlasapi.feeds.youview.amazon.AmazonBroadcastEventGenerator newAmazonBroadcastGenerator() {
        return new org.atlasapi.feeds.youview.amazon.AmazonBroadcastEventGenerator();
    }
    @Bean
    public org.atlasapi.feeds.youview.amazon.AmazonChannelGenerator newAmazonChannelGenerator() {
        return new org.atlasapi.feeds.youview.amazon.AmazonChannelGenerator();
    }

    @Bean
    public org.atlasapi.feeds.youview.amazon.AmazonMasterbrandGenerator newAmazonMasterbrandGenerator() {
        return new org.atlasapi.feeds.youview.amazon.AmazonMasterbrandGenerator();
    }
    @Bean
    public org.atlasapi.feeds.youview.amazon.AmazonIdGenerator newAmazonIdGenerator() {
        return new org.atlasapi.feeds.youview.amazon.AmazonIdGenerator();
    }

    @Bean
    public org.atlasapi.feeds.youview.amazon.AmazonGenreMapping newAmazonGenreMapping() {
        return new org.atlasapi.feeds.youview.amazon.AmazonGenreMapping();
    }

    @Bean
    public org.atlasapi.feeds.youview.amazon.AmazonBroadcastServiceMapping newAmazonBroadcastServiceMapping() {
        return new org.atlasapi.feeds.youview.amazon.AmazonBroadcastServiceMapping();
    }

    public ServiceIdResolver newAmazonServiceIdResolver() {
        return new org.atlasapi.feeds.youview.amazon.AmazonServiceIdResolver();
    }

    @Bean
    public ContentHierarchyExpander newAmazonContentHierarchyExpander() {
        return new ContentHierarchyExpanderImpl(newVersionHierarchyExpander(), newBroadcastHierarchyExpander(), newOnDemandHierarchyExpander(), newAmazonIdGenerator());
    }

    public VersionHierarchyExpander newVersionHierarchyExpander() {
        return new VersionHierarchyExpander(newAmazonIdGenerator());
    }

    public OnDemandHierarchyExpander newOnDemandHierarchyExpander() {
        return new OnDemandHierarchyExpander(newAmazonIdGenerator());
    }

    public BroadcastHierarchyExpander newBroadcastHierarchyExpander() {
        return new BroadcastHierarchyExpander(newAmazonIdGenerator(), newAmazonBroadcastServiceMapping(), newAmazonServiceIdResolver() , new SystemClock());
    }
}
