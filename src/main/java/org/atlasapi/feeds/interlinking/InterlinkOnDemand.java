package org.atlasapi.feeds.interlinking;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class InterlinkOnDemand extends InterlinkBase {

	private final DateTime availabilityStart;
    private final DateTime availabilityEnd;
    private final Duration duration;
    private String service;
    private final String parentId;
	private final String uri;

    public InterlinkOnDemand(String id, String uri, Operation operation, DateTime availabilityStart, DateTime availabilityEnd, Duration duration, String parentId) {
		super(id, operation);
		this.uri = uri;
        this.availabilityStart = availabilityStart;
        this.availabilityEnd = availabilityEnd;
        this.duration = duration;
        this.parentId = parentId;
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
	
	public String parentId() {
        return parentId;
    }

	public String uri() {
		return uri;
	}
}
