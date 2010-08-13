package org.atlasapi.feeds.interlinking;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class InterlinkOnDemand extends InterlinkBase {

	private final DateTime availabilityStart;
    private final DateTime availabilityEnd;
    private final Duration duration;
    private String service;
    private final InterlinkEpisode episode;

    public InterlinkOnDemand(String id, DateTime availabilityStart, DateTime availabilityEnd, Duration duration, InterlinkEpisode episode) {
		super(id);
        this.availabilityStart = availabilityStart;
        this.availabilityEnd = availabilityEnd;
        this.duration = duration;
        this.episode = episode;
	}
	
	public InterlinkOnDemand withLastUpdated(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }
	
	public InterlinkOnDemand withService(String service) {
	    this.service = service;
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
	
	public String service() {
	    return service;
	}
	
	public InterlinkEpisode episode() {
        return episode;
    }
}
