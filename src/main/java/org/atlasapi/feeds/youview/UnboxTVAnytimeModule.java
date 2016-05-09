package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.unbox.UnboxBroadcastEventGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxBroadcastServiceMapping;
import org.atlasapi.feeds.youview.unbox.UnboxChannelGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxGenreMapping;
import org.atlasapi.feeds.youview.unbox.UnboxGroupInformationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxIdGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxOnDemandLocationGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxProgramInformationGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class UnboxTVAnytimeModule {
    
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
}
