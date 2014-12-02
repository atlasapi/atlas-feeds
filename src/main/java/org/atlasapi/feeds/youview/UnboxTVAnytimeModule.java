package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastServiceMapping;
import org.atlasapi.feeds.youview.unbox.UnboxGenreMapping;
import org.atlasapi.feeds.youview.unbox.UnboxGroupInformationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxIdGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxProgramInformationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxTvAnytimeElementCreator;
import org.atlasapi.persistence.content.ContentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class UnboxTVAnytimeModule {

    private @Autowired ContentResolver contentResolver;
    
    @Bean
    public UnboxTvAnytimeElementCreator unboxElementCreator() {
        return new UnboxTvAnytimeElementCreator(
                new UnboxProgramInformationGenerator(unboxIdGenerator(), versionHierarchyExpander()), 
                new UnboxGroupInformationGenerator(unboxIdGenerator(), unboxGenreMapping()), 
                new UnboxOnDemandLocationGenerator(unboxIdGenerator()), 
                contentHierarchy(), 
                new UriBasedContentPermit()
        );
    }
    
    private VersionHierarchyExpander versionHierarchyExpander() {
        return new VersionHierarchyExpander(unboxIdGenerator());
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

    private ContentHierarchyExtractor contentHierarchy() {
        return new ContentResolvingContentHierarchyExtractor(contentResolver);
    }
}
