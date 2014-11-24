package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.ChannelResolvingBbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastEventGenerator;
import org.atlasapi.feeds.youview.nitro.NitroBroadcastServiceMapping;
import org.atlasapi.feeds.youview.nitro.NitroGenreMapping;
import org.atlasapi.feeds.youview.nitro.NitroGroupInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroProgramInformationGenerator;
import org.atlasapi.feeds.youview.nitro.NitroTvAnytimeElementCreator;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.content.ContentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.hash.Hashing;


@Configuration
public class NitroTVAnytimeModule {

    private @Autowired ContentResolver contentResolver;
    private @Autowired ChannelResolver channelResolver;
    
    @Bean
    public NitroTvAnytimeElementCreator nitroElementCreator() {
        TvAnytimeElementFactory elementFactory = TvAnytimeElementFactory.INSTANCE;
        
        return new NitroTvAnytimeElementCreator(
                new NitroProgramInformationGenerator(nitroIdGenerator(), elementFactory), 
                new NitroGroupInformationGenerator(nitroIdGenerator(), nitroGenreMapping(), bbcServiceIdResolver()), 
                new NitroOnDemandLocationGenerator(nitroIdGenerator(), elementFactory), 
                new NitroBroadcastEventGenerator(nitroIdGenerator(), elementFactory, nitroServiceMapping(), bbcServiceIdResolver()),
                contentHierarchy(), 
                new UriBasedContentPermit()
        );
    }
    
    @Bean
    public NitroBroadcastServiceMapping nitroServiceMapping() {
        return new NitroBroadcastServiceMapping("nitro_service_mapping.csv");
    }
    
    @Bean
    public NitroIdGenerator nitroIdGenerator() {
        return new NitroIdGenerator(Hashing.md5());
    }
    
    @Bean
    public NitroGenreMapping nitroGenreMapping() {
        return new NitroGenreMapping();
    }

    @Bean
    public BbcServiceIdResolver bbcServiceIdResolver() {
        return new ChannelResolvingBbcServiceIdResolver(channelResolver);
    }

    private ContentHierarchyExtractor contentHierarchy() {
        return new ContentResolvingContentHierarchyExtractor(contentResolver);
    }
}
