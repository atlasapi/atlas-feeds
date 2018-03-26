package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.nitro.NitroServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastEventGenerator;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastServiceMapping;
import org.atlasapi.feeds.youview.nitro.NitroChannelInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroCreditsItemGenerator;
import org.atlasapi.feeds.youview.nitro.NitroEpisodeNumberPrefixAddingContentTitleGenerator;
import org.atlasapi.feeds.youview.nitro.NitroGenreMapping;
import org.atlasapi.feeds.youview.nitro.NitroGroupInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroMasterbrandInfoGenerator;
import org.atlasapi.feeds.youview.nitro.NitroOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroProgramInformationGenerator;
import org.atlasapi.feeds.youview.persistence.IdMappingStore;
import org.atlasapi.feeds.youview.persistence.StoringMappingIdGenerator;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.content.PeopleResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.time.SystemClock;

@Configuration
public class NitroTVAnytimeModule {

    private @Autowired ChannelResolver channelResolver;
    private @Autowired IdMappingStore idMappingStore;
    private @Autowired PeopleResolver peopleResolver;

    @Bean
    public NitroProgramInformationGenerator nitroProgInfoGenerator() {
        return new NitroProgramInformationGenerator(nitroIdGenerator());
    }
    
    @Bean
    public NitroGroupInformationGenerator nitroGroupInfoGenerator() {
        return new NitroGroupInformationGenerator(
                        nitroIdGenerator(), 
                        nitroGenreMapping(), 
                        bbcServiceIdResolver(), 
                        nitroCreditsGenerator(), 
                        titleGenerator()
                   );
    }

    @Bean
    public NitroOnDemandLocationGenerator nitroOnDemandGenerator() {
        return new NitroOnDemandLocationGenerator(nitroIdGenerator());
    }
    
    @Bean 
    public NitroBroadcastEventGenerator nitroBroadcastGenerator() {
        return new NitroBroadcastEventGenerator(nitroIdGenerator());
    }

    @Bean
    public NitroChannelInformationGenerator nitroChannelGenerator() {
        return new NitroChannelInformationGenerator();
    }

    @Bean
    public NitroMasterbrandInfoGenerator nitroMasterbrandInfoGenerator() {
        return new NitroMasterbrandInfoGenerator();
    }

    @Bean
    public NitroEpisodeNumberPrefixAddingContentTitleGenerator titleGenerator() {
        return new NitroEpisodeNumberPrefixAddingContentTitleGenerator();
    }

    @Bean
    public NitroBroadcastServiceMapping nitroServiceMapping() {
        return new NitroBroadcastServiceMapping("nitro_service_mapping.csv");
    }

    private StoringMappingIdGenerator nitroIdGenerator() {
        return new StoringMappingIdGenerator(idMappingStore, new NitroIdGenerator());
    }
    
    @Bean
    public NitroGenreMapping nitroGenreMapping() {
        return new NitroGenreMapping();
    }

    @Bean
    public NitroCreditsItemGenerator nitroCreditsGenerator() {
        return new NitroCreditsItemGenerator(peopleResolver);
    }

    @Bean
    public NitroServiceIdResolver bbcServiceIdResolver() {
        return new NitroServiceIdResolver(channelResolver);
    }

    @Bean
    public ContentHierarchyExpander nitroContentHierarchyExpander() {
        return new ContentHierarchyExpanderImpl(versionHierarchyExpander(), broadcastHierarchyExpander(), onDemandHierarchyExpander(), nitroIdGenerator());
    }

    public VersionHierarchyExpander versionHierarchyExpander() {
        return new VersionHierarchyExpander(nitroIdGenerator());
    }

    public OnDemandHierarchyExpander onDemandHierarchyExpander() {
        return new OnDemandHierarchyExpander(nitroIdGenerator());
    }

    public BroadcastHierarchyExpander broadcastHierarchyExpander() {
        return new BroadcastHierarchyExpander(nitroIdGenerator(), nitroServiceMapping(), bbcServiceIdResolver(), new SystemClock());
    }

}
