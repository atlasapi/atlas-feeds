package org.atlasapi.feeds.interlinking;

import java.util.Set;

import org.joda.time.DateTime;

import com.google.common.collect.Sets;

public class InterlinkEpisode extends InterlinkContent {

	private final Set<InterlinkBroadcast> broadcasts = Sets.newHashSet();
	private final Set<InterlinkOnDemand> onDemands = Sets.newHashSet();
	
	public InterlinkEpisode(String id, Integer index) {
		super(id, index);
	}
	
	public InterlinkEpisode withTitle(String title) {
		this.title = title;
		return this;
	}

	public InterlinkEpisode addBroadcast(InterlinkBroadcast interlinkBroadcast) {
		broadcasts.add(interlinkBroadcast);
		return this;
	}

	public Set<InterlinkBroadcast> broadcasts() {
		return broadcasts;
	}

	public InterlinkEpisode addOnDemand(InterlinkOnDemand onDemand) {
		onDemands.add(onDemand);
		return this;
	}

	public Set<InterlinkOnDemand> onDemands() {
		return onDemands;
	}

	public InterlinkEpisode withSummary(String summary) {
		this.summary = summary;
		return this;
	}
	
	public InterlinkEpisode withLastUpdated(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }
	
	public InterlinkEpisode withDescription(String description) {
		this.description = description;
		return this;
	}
}
