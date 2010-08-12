package org.atlasapi.feeds.interlinking;

import org.joda.time.DateTime;

public class InterlinkOnDemand extends InterlinkBase {

	public InterlinkOnDemand(String id) {
		super(id);
	}
	
	public InterlinkOnDemand withLastUpdated(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }
}
