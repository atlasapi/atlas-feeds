package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.ChannelResolvingBbcServiceIdResolver;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastEventGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastServiceMapping;
import org.atlasapi.feeds.youview.unbox.UnboxChannelGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxContentHierarchyExpander;
import org.atlasapi.feeds.youview.unbox.UnboxGenreMapping;
import org.atlasapi.feeds.youview.unbox.UnboxGroupInformationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxIdGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxMasterbrandGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxOnDemandLocationGenerator;
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
    public UnboxOnDemandLocationGenerator unboxOnDemandGenerator() {
        return new UnboxOnDemandLocationGenerator(unboxIdGenerator());
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
    public UnboxIdGenerator unboxIdGenerator() {
        return new UnboxIdGenerator();
    }
    
    @Bean
    public UnboxGenreMapping unboxGenreMapping() {
        return new UnboxGenreMapping();
    }
    
    @Bean
    public UnboxBroadcastServiceMapping unboxBroadcastServiceMapping() {
        return new UnboxBroadcastServiceMapping();
    }

    @Bean
    public BbcServiceIdResolver bbcServiceIdResolver() {
        return new ChannelResolvingBbcServiceIdResolver(channelResolver);
    }

    @Bean
    public ContentHierarchyExpander contentHierarchyExpander() {
        return new UnboxContentHierarchyExpander(versionHierarchyExpander(), broadcastHierarchyExpander(), onDemandHierarchyExpander(), unboxIdGenerator());
    }

    @Bean
    public VersionHierarchyExpander versionHierarchyExpander() {
        return new VersionHierarchyExpander(unboxIdGenerator());
    }

    @Bean
    public OnDemandHierarchyExpander onDemandHierarchyExpander() {
        return new OnDemandHierarchyExpander(unboxIdGenerator());
    }

    @Bean
    public BroadcastHierarchyExpander broadcastHierarchyExpander() {
        return new BroadcastHierarchyExpander(unboxIdGenerator(), unboxBroadcastServiceMapping(), bbcServiceIdResolver() , new SystemClock());
    }
}
