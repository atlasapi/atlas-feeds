package org.atlasapi.feeds.xmltv;

import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.KnownTypeContentResolver;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XmlTvModule {
    
    @Autowired ScheduleResolver scheduleResolver;
    @Autowired KnownTypeContentResolver contentResolver;

    public @Bean XmlTvController xmlTvController() {
        return new XmlTvController(new XmlTvFeedCompiler(scheduleResolver, contentResolver, Publisher.PA), new XmlTvChannelLookup());
    }
    
    static final String FEED_PREABMLE = "\t\nIn accessing this XML feed, you agree that you will only access its contents for your own personal " +
    		"and non-commercial use and not for any commercial or other purposes, including advertising or selling any goods or services, " +
    		"including any third-party software applications available to the general public.";
    
}
