package org.atlasapi.feeds.interlinking;

import java.util.Set;

import com.google.common.collect.Sets;

public class InterlinkBrand extends InterlinkContent {

	private final Set<InterlinkSeries> series = Sets.newHashSet();

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
}
