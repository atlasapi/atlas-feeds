package org.atlasapi.feeds.youview.statistics;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;


public interface FeedStatisticsWriter {

    void updateFeedStatistics(Publisher publisher, Duration latency);
}
