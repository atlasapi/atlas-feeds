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
    
    public SinceLastUpdatedInterlinkingDeltaUpdater(InterlinkingDeltaUpdater updater) {
        this.updater = updater;
    }

    @Override
    protected void runTask() {
        DateTime now = clock.now();
        DateTime startOfDay = now.withTime(0, 0, 0, 0);
        DateTime endOfDay = startOfDay.plusDays(1);
        
        try {
            Maybe<Document> existingFeedElement = updater.getExistingFeedElement(now);
            
            if (existingFeedElement.hasValue()) {
                updater.updateFeed(existingFeedElement, updater.getLastUpdated(existingFeedElement.requireValue()), endOfDay);
            } else {
                DateTime yesterday = now.minusDays(1);
                Maybe<Document> existingPreviousDayFeed = updater.getExistingFeedElement(yesterday);
                if (existingPreviousDayFeed.hasValue()) {
                    updater.updateFeed(existingPreviousDayFeed, updater.getLastUpdated(existingPreviousDayFeed.requireValue()), startOfDay);
                }
                
                updater.updateFeed(Maybe.<Document>nothing(), startOfDay, endOfDay);
            }
        }
        catch (Exception e) {
            log.error("Exception when updating interlinking", e);
        }
    }
}
