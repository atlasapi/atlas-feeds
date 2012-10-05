package org.atlasapi.feeds.interlinking;

import org.atlasapi.feeds.interlinking.www.InterlinkController;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;

@Configuration
public class InterlinkingModule {
    
	private @Autowired LastUpdatedContentFinder executor;
	
	private @Autowired ChannelResolver channelResolver;

	public @Bean InterlinkController feedController() {
		return new InterlinkController(executor, ImmutableMap.<Publisher, PlaylistToInterlinkFeed>of(Publisher.C4, new C4PlaylistToInterlinkFeedAdapter()), channelResolver);
	}
	
}
