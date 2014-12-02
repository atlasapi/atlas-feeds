package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmBroadcastServiceMapping;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmGenreMapping;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmGroupInformationGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmIdGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmProgramInformationGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmTvAnytimeElementCreator;
import org.atlasapi.persistence.content.ContentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class LoveFilmTVAnytimeModule {

    private @Autowired ContentResolver contentResolver;
    
    @Bean
    public LoveFilmTvAnytimeElementCreator loveFilmElementCreator() {
        return new LoveFilmTvAnytimeElementCreator(
                new LoveFilmProgramInformationGenerator(loveFilmIdGenerator(), versionHierarchyExpander()), 
                new LoveFilmGroupInformationGenerator(loveFilmIdGenerator(), loveFilmGenreMapping()), 
                new LoveFilmOnDemandLocationGenerator(loveFilmIdGenerator()), 
                contentHierarchy(), 
                new UriBasedContentPermit()
        );
    }
    
    private VersionHierarchyExpander versionHierarchyExpander() {
        return new VersionHierarchyExpander(loveFilmIdGenerator());
    }

    @Bean
    public LoveFilmIdGenerator loveFilmIdGenerator() {
        return new LoveFilmIdGenerator();
    }
    
    @Bean
    public LoveFilmGenreMapping loveFilmGenreMapping() {
        return new LoveFilmGenreMapping();
    }
    
    @Bean
    public LoveFilmBroadcastServiceMapping loveFilmBroadcastServiceMapping() {
        return new LoveFilmBroadcastServiceMapping();
    }

    private ContentHierarchyExtractor contentHierarchy() {
        return new ContentResolvingContentHierarchyExtractor(contentResolver);
    }
}
