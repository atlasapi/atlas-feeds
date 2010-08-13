package org.atlasapi.feeds.interlinking;

import org.joda.time.DateTime;

public class InterlinkBrand extends InterlinkContent {

	public InterlinkBrand(String id) {
		super(id, null);
	}
	
	public InterlinkBrand withTitle(String title) {
		this.title = title;
		return this;
	}
	
	public InterlinkBrand withLastUpdated(DateTime lastUpdated) {
	    this.lastUpdated = lastUpdated;
	    return this;
	}
	
	public InterlinkBrand withThumbnail(String thumbnail) {
	    this.thumbnail = thumbnail;
	    return this;
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
