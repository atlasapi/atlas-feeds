package org.atlasapi.feeds.interlinking;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class InterlinkBroadcast extends InterlinkBase {

	private DateTime broadcastStart;
	private Duration duration;

	public InterlinkBroadcast(String id) {
		super(id);
	}

	public InterlinkBroadcast withBroadcastStart(DateTime broadcastStart) {
		this.broadcastStart = broadcastStart;
		return this;
	}
	
	public DateTime broadcastStart() {
		return broadcastStart;
	}

	public InterlinkBroadcast withDuration(Duration duration) {
		this.duration = duration;
		return this;
	}
	
	public Duration duration() {
		return duration;
	}
	
	public InterlinkBroadcast withLastUpdated(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated();
        return this;
    }
}
