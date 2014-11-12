package org.atlasapi.feeds.youview.statistics;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;


public interface FeedStatisticsWriter {

    void save(FeedStatistics feedStatistics);
    void updateQueueSize(Publisher publisher, int queueSize);
    void updateAverageLatency(Publisher publisher, Duration latency);
}
