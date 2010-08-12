package org.atlasapi.feeds.interlinking;

import java.util.Set;

import org.joda.time.DateTime;

import com.google.common.collect.Sets;

public class InterlinkSeries extends InterlinkContent {

	private final Set<InterlinkEpisode> episodes = Sets.newHashSet();
	
	public InterlinkSeries(String id, Integer index) {
		super(id, index);
	}
	
	public InterlinkSeries addEpisode(InterlinkEpisode episode) {
		episodes.add(episode);
		return this;
	}
	
	public InterlinkSeries withTitle(String title) {
		this.title = title;
		return this;
	}

	public Set<InterlinkEpisode> episodes() {
		return episodes;
	}
	
	public InterlinkSeries withSummary(String summary) {
		this.summary = summary;
		return this;
	}
	
	public InterlinkSeries withLastUpdated(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated();
        return this;
    }
}
