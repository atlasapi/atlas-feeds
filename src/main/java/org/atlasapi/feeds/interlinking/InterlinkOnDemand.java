package org.atlasapi.feeds.interlinking;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class InterlinkOnDemand extends InterlinkBase {

	private final DateTime availabilityStart;
    private final DateTime availabilityEnd;
    private final Duration duration;

    public InterlinkOnDemand(String id, DateTime availabilityStart, DateTime availabilityEnd, Duration duration) {
		super(id);
        this.availabilityStart = availabilityStart;
        this.availabilityEnd = availabilityEnd;
        this.duration = duration;
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
	
	public Duration duration() {
	    return duration;
	}
}
