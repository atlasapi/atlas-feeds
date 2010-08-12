package org.atlasapi.feeds.interlinking;

import java.util.Set;

import org.joda.time.DateTime;

import com.google.common.collect.Sets;

public class InterlinkBrand extends InterlinkContent {

	private final Set<InterlinkSeries> series = Sets.newHashSet();
	private final Set<InterlinkEpisode> episodes = Sets.newHashSet();

	public InterlinkBrand(String id) {
		super(id, null);
	}
	
	public Set<InterlinkSeries> series() {
		return series;
	}
	
	public InterlinkBrand addSeries(InterlinkSeries series) {
		this.series.add(series);
		return this;
	}
	
	public InterlinkBrand withTitle(String title) {
		this.title = title;
		return this;
	}
	
	public InterlinkBrand withLastUpdated(DateTime lastUpdated) {
	    this.lastUpdated = lastUpdated;
	    return this;
	}

	public InterlinkBrand addEpisodeWithoutASeries(InterlinkEpisode episode) {
		episodes.add(episode);
		return this;
	}
	
	public Set<InterlinkEpisode> episodesWithoutASeries() {
		return episodes;
	}
	
	public InterlinkBrand withSummary(String summary) {
		this.summary = summary;
		return this;
	}
	
	public InterlinkBrand withDescription(String description) {
		this.description = description;
		return this;
	}
}
