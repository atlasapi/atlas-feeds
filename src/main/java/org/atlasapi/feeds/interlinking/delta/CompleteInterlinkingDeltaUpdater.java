package org.atlasapi.feeds.interlinking.delta;

import nu.xom.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.SystemClock;

public class CompleteInterlinkingDeltaUpdater extends ScheduledTask {
    
    private final Log log = LogFactory.getLog(getClass());
    private final InterlinkingDeltaUpdater updater;
    private final int days;

    public CompleteInterlinkingDeltaUpdater(InterlinkingDeltaUpdater updater, int days) {
        this.updater = updater;
        this.days = days;
    }

    @Override
    protected void runTask() {
        DateTime now = new SystemClock().now();
        
        for (int i = days; i >= 0; i--) {
            try {
                DateTime day = now.minusDays(i);
                Maybe<Element> existingFeedElement = updater.getExistingFeedElement(day);
                if (existingFeedElement.isNothing()) {
                    DateTime startOfDay = day.withTime(0, 0, 0, 0);
                    updater.updateFeed(Maybe.<Element>nothing(), startOfDay, startOfDay.plusDays(1));
                }
            } catch (Exception e) {
                log.error("Exception when updating interlinking deltas", e);
            }
        }
    }
}
