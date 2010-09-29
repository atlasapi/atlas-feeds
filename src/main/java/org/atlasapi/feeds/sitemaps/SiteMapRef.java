package org.atlasapi.feeds.sitemaps;

import org.joda.time.DateTime;

public class SiteMapRef {

	private final String url;
	private final DateTime lastModified;

	public SiteMapRef(String url, DateTime lastModified) {
		this.url = url;
		this.lastModified = lastModified;
	}
	
	public String getUrl() {
		return url;
	}

	public DateTime getLastModified() {
		return lastModified;
	}
}
