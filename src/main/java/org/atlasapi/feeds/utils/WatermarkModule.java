package org.atlasapi.feeds.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

@Configuration
public class WatermarkModule {

    private static final Splitter SPLITTER = Splitter.on("|");
    
    @Value("${watermark.channelUris}")
    private String channelsToWatermark;
    
    @Bean
    DescriptionWatermarker descriptionWatermarker() {
        return new DescriptionWatermarker(SPLITTER.split(Strings.nullToEmpty(channelsToWatermark)));
    }
}
