package org.atlasapi.feeds.interlinking.delta;

import nu.xom.Document;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.SystemClock;

public class CompleteInterlinkingDeltaUpdater extends ScheduledTask {
    
    private final Log log = LogFactory.getLog(getClass());
    private final InterlinkingDeltaUpdater updater;
    private final InterlinkingDeltaStore store;
    private final int days;

    public CompleteInterlinkingDeltaUpdater(InterlinkingDeltaStore store, InterlinkingDeltaUpdater updater, int days) {
        this.store = store;
        this.updater = updater;
        this.days = days;
    }

    @Override
    protected void runTask() {
        DateTime now = new SystemClock().now();
        
        for (int i = days; i >= 0; i--) {
            try {
                DateTime day = now.minusDays(i);
                Maybe<Document> existingFeedElement = store.getExistingFeedElement(day);
                if (existingFeedElement.isNothing()) {
                    DateTime startOfDay = day.withTime(0, 0, 0, 0);
                    store.store(startOfDay, updater.updateFeed(Maybe.<Document>nothing(), startOfDay, startOfDay.plusDays(1)));
                }
            } catch (Exception e) {
                log.error("Exception when updating interlinking deltas", e);
            }
        }
    }
}
