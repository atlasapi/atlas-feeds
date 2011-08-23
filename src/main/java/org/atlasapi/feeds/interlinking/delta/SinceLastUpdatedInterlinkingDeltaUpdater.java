package org.atlasapi.feeds.interlinking.delta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;

public class SinceLastUpdatedInterlinkingDeltaUpdater extends ScheduledTask {
    
    private final Log log = LogFactory.getLog(getClass());
    private final Clock clock = new SystemClock();
    
    private final InterlinkingDeltaUpdater updater;
    private final InterlinkingDeltaStore store; 
    
    public SinceLastUpdatedInterlinkingDeltaUpdater(InterlinkingDeltaStore store, InterlinkingDeltaUpdater updater) {
        this.store = store;
        this.updater = updater;
    }

    @Override
    protected void runTask() {
        DateTime now = clock.now();
        DateTime startOfDay = now.withTime(0, 0, 0, 0);
        DateTime endOfDay = startOfDay.plusDays(1);
        
        try {
            InterlinkingDelta deltaForToday = store.getExistingDelta(now);
            
            if (deltaForToday.exists()) {
                //update the feed for today.
                InterlinkingDelta updatedFeed = updater.updateFeed(deltaForToday, endOfDay);
                store.storeDelta(now, updatedFeed);
            } else {
                //finalize feed for yesterday.
                DateTime yesterday = now.minusDays(1);
                InterlinkingDelta deltaForYesterday = store.getExistingDelta(yesterday);
                if (deltaForYesterday.exists()) {
                    InterlinkingDelta yesterdaysUpdatedDelta = updater.updateFeed(deltaForYesterday, startOfDay);
                    store.storeDelta(yesterday, yesterdaysUpdatedDelta);
                }
                // and create new feed for today.
                InterlinkingDelta todaysUpdatedFeed = updater.updateFeed(deltaForToday, startOfDay, endOfDay);
                store.storeDelta(now, todaysUpdatedFeed);
            }
        }
        catch (Exception e) {
            log.error("Exception when updating interlinking", e);
        }
    }
}
