package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.unbox.AmazonIdGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastEventGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastServiceMapping;
import org.atlasapi.feeds.youview.unbox.UnboxChannelGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxServiceIdResolver;
import org.atlasapi.feeds.youview.unbox.UnboxGenreMapping;
import org.atlasapi.feeds.youview.unbox.UnboxGroupInformationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxMasterbrandGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxProgramInformationGenerator;
import org.atlasapi.media.channel.ChannelResolver;

import com.metabroadcast.common.time.SystemClock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class UnboxTVAnytimeModule {

    private @Autowired ChannelResolver channelResolver;
    
    @Bean
    public UnboxProgramInformationGenerator unboxProgInfoGenerator() {
        return new UnboxProgramInformationGenerator(unboxIdGenerator());
    }
    
    @Bean
    public UnboxGroupInformationGenerator unboxGroupInfoGenerator() {
        return new UnboxGroupInformationGenerator(unboxIdGenerator(), unboxGenreMapping());
    }
    
    @Bean
    public AmazonOnDemandLocationGenerator unboxOnDemandGenerator() {
        return new AmazonOnDemandLocationGenerator(unboxIdGenerator());
    }
    
    @Bean
    public UnboxBroadcastEventGenerator unboxBroadcastGenerator() {
        return new UnboxBroadcastEventGenerator();
    }
    @Bean
    public UnboxChannelGenerator unboxChannelGenerator() {
        return new UnboxChannelGenerator();
    }

    @Bean
    public UnboxMasterbrandGenerator unboxMasterbrandGenerator() {
        return new UnboxMasterbrandGenerator();
    }
    @Bean
    public AmazonIdGenerator unboxIdGenerator() {
        return new AmazonIdGenerator();
    }
    
    @Bean
    public UnboxGenreMapping unboxGenreMapping() {
        return new UnboxGenreMapping();
    }
    
    @Bean
    public UnboxBroadcastServiceMapping unboxBroadcastServiceMapping() {
        return new UnboxBroadcastServiceMapping();
    }

    public ServiceIdResolver unboxServiceIdResolver() {
        return new UnboxServiceIdResolver();
    }

    @Bean
    public ContentHierarchyExpander unboxContentHierarchyExpander() {
        return new ContentHierarchyExpanderImpl(versionHierarchyExpander(), broadcastHierarchyExpander(), onDemandHierarchyExpander(), unboxIdGenerator());
    }

    public VersionHierarchyExpander versionHierarchyExpander() {
        return new VersionHierarchyExpander(unboxIdGenerator());
    }

    public OnDemandHierarchyExpander onDemandHierarchyExpander() {
        return new OnDemandHierarchyExpander(unboxIdGenerator());
    }

    public BroadcastHierarchyExpander broadcastHierarchyExpander() {
        return new BroadcastHierarchyExpander(unboxIdGenerator(), unboxBroadcastServiceMapping(), unboxServiceIdResolver() , new SystemClock());
    }
}
