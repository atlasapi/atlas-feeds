package org.atlasapi.feeds.interlinking.delta;

import nu.xom.Document;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.metabroadcast.common.base.Maybe;
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
            Maybe<Document> existingFeedElement = store.getExistingFeedElement(now);
            
            if (existingFeedElement.hasValue()) {
                //update the feed for today.
                Document updatedFeed = updater.updateFeed(existingFeedElement, endOfDay);
                store.store(now, updatedFeed);
            } else {
                //finalize feed for yesterday.
                DateTime yesterday = now.minusDays(1);
                Maybe<Document> existingPreviousDayFeed = store.getExistingFeedElement(yesterday);
                if (existingPreviousDayFeed.hasValue()) {
                    Document yesterdaysFeed = updater.updateFeed(existingPreviousDayFeed, startOfDay);
                    store.store(yesterday, yesterdaysFeed);
                }
                // and create new feed for today.
                Document todaysFeed = updater.updateFeed(Maybe.<Document>nothing(), startOfDay, endOfDay);
                store.store(now, todaysFeed);
            }
        }
        catch (Exception e) {
            log.error("Exception when updating interlinking", e);
        }
    }
}
