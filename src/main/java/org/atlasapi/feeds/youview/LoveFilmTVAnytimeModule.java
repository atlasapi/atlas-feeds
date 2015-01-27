package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.lovefilm.LoveFilmBroadcastEventGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmBroadcastServiceMapping;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmGenreMapping;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmGroupInformationGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmIdGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.lovefilm.LoveFilmProgramInformationGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class LoveFilmTVAnytimeModule {

    @Bean
    public LoveFilmProgramInformationGenerator loveFilmProgInfoGenerator() {
        return new LoveFilmProgramInformationGenerator(loveFilmIdGenerator());
    }
    
    @Bean
    public LoveFilmGroupInformationGenerator loveFilmGroupInfoGenerator() {
        return new LoveFilmGroupInformationGenerator(loveFilmIdGenerator(), loveFilmGenreMapping());
    }
    
    @Bean
    public LoveFilmOnDemandLocationGenerator loveFilmOnDemandGenerator() {
        return new LoveFilmOnDemandLocationGenerator(loveFilmIdGenerator());
    }
    
    @Bean
    public LoveFilmBroadcastEventGenerator loveFilmBroadcastGenerator() {
        return new LoveFilmBroadcastEventGenerator();
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
}
