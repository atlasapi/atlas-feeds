package org.atlasapi.feeds.interlinking;

import org.atlasapi.feeds.interlinking.delta.CompleteInterlinkingDeltaUpdater;
import org.atlasapi.feeds.interlinking.delta.InterlinkingDeltaUpdater;
import org.atlasapi.feeds.interlinking.delta.SinceLastUpdatedInterlinkingDeltaUpdater;
import org.atlasapi.feeds.interlinking.outputting.InterlinkFeedOutputter;
import org.atlasapi.feeds.interlinking.www.InterlinkController;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.jets3t.service.security.AWSCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
public class InterlinkingModule {
    
    private @Value("${s3.access}") String s3access;
    private @Value("${s3.secret}") String s3secret;

	private @Autowired ContentResolver resolver;
	private @Autowired LastUpdatedContentFinder executor;
	
	@Autowired
	private SimpleScheduler scheduler;

	public @Bean InterlinkController feedController() {
		return new InterlinkController(executor, ImmutableMap.<Publisher, PlaylistToInterlinkFeed>of(Publisher.C4, new C4PlaylistToInterlinkFeedAdapter(resolver)));
	}
	
	@Bean
	public InterlinkingDeltaUpdater interlinkingDeltaUpdater() {
	    return new InterlinkingDeltaUpdater(new AWSCredentials(s3access, s3secret), executor, new InterlinkFeedOutputter(), new C4PlaylistToInterlinkFeedAdapter(resolver));
	}
	
	@Bean 
	public CompleteInterlinkingDeltaUpdater completeInterlinkingDeltaUpdater() {
	    
	    CompleteInterlinkingDeltaUpdater completeInterlinkingDeltaUpdater = new CompleteInterlinkingDeltaUpdater(interlinkingDeltaUpdater(), 30);
	    
	    scheduler.schedule(completeInterlinkingDeltaUpdater.withName("Complete interlinking deltas updater"), RepetitionRules.NEVER);
	    
	    return completeInterlinkingDeltaUpdater;
	}
	
	@Bean
	public SinceLastUpdatedInterlinkingDeltaUpdater sinceLastUpdatedInterlinkingDeltaUpdater() {
	    SinceLastUpdatedInterlinkingDeltaUpdater sinceLastUpdatedInterlinkingDeltaUpdater = new SinceLastUpdatedInterlinkingDeltaUpdater(interlinkingDeltaUpdater());
	    
	    scheduler.schedule(sinceLastUpdatedInterlinkingDeltaUpdater.withName("Last updated interlink deltas updater"), RepetitionRules.NEVER);
	    
	    return sinceLastUpdatedInterlinkingDeltaUpdater;
	}
	
}
