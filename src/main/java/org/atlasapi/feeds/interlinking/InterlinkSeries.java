package org.atlasapi.feeds.interlinking;

import org.joda.time.DateTime;

public class InterlinkSeries extends InterlinkContent {

	private final InterlinkBrand brand;
	
	public InterlinkSeries(String id, Integer index, InterlinkBrand brand) {
		super(id, index);
        this.brand = brand;
	}
	
	public InterlinkSeries withTitle(String title) {
		this.title = title;
		return this;
	}
	
	public InterlinkSeries withSummary(String summary) {
		this.summary = summary;
		return this;
	}
	
	public InterlinkSeries withLastUpdated(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }
	
	public InterlinkSeries withThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
        return this;
    }
	
	public InterlinkSeries withDescription(String description) {
		this.description = description;
		return this;
	}
	
	public InterlinkBrand brand() {
        return brand;
    }
}
