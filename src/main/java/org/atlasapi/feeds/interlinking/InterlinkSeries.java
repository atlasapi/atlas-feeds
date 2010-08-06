package org.atlasapi.feeds.interlinking;

import java.util.Set;

import com.google.common.collect.Sets;

public class InterlinkSeries extends InterlinkContent {

	private final Set<InterlinkEpisode> episodes = Sets.newHashSet();
	
	public InterlinkSeries(String id, int index) {
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
}
