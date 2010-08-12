package org.atlasapi.feeds.interlinking;

import org.joda.time.DateTime;

public class InterlinkOnDemand extends InterlinkBase {

	private final DateTime availabilityStart;
    private final DateTime availabilityEnd;

    public InterlinkOnDemand(String id, DateTime availabilityStart, DateTime availabilityEnd) {
		super(id);
        this.availabilityStart = availabilityStart;
        this.availabilityEnd = availabilityEnd;
	}
	
	public InterlinkOnDemand withLastUpdated(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }
	
	public DateTime availabilityStart() {
        return availabilityStart;
    }
	
	public DateTime availabilityEnd() {
        return availabilityEnd;
    }
}
