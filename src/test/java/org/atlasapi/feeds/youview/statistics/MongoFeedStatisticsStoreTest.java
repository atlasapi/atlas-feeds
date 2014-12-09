package org.atlasapi.feeds.youview.statistics;

import static org.junit.Assert.assertEquals;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;
import org.junit.Test;

import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class MongoFeedStatisticsStoreTest {
    
    private static final Duration LATENCY = Duration.standardDays(1);
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final int QUEUE_SIZE = 100;

    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    
    private final MongoFeedStatisticsStore store = new MongoFeedStatisticsStore(mongo);

    @Test
    public void testSaveAndResolutionOfStats() {
        store.updateFeedStatistics(PUBLISHER, QUEUE_SIZE, LATENCY);
        
        FeedStatistics resolved = store.resolveFor(PUBLISHER).get();
        
        assertEquals(PUBLISHER, resolved.publisher());
        assertEquals(QUEUE_SIZE, resolved.queueSize());
        assertEquals(LATENCY, resolved.updateLatency());
    }

    @Test
    public void testUpdateOfFeedStatistics() {
        store.updateFeedStatistics(PUBLISHER, QUEUE_SIZE, LATENCY);
        
        int newQueueSize = QUEUE_SIZE + 50;
        Duration newLatency = LATENCY.plus(Duration.standardHours(10));
        
        store.updateFeedStatistics(PUBLISHER, newQueueSize, newLatency);
        
        FeedStatistics resolved = store.resolveFor(PUBLISHER).get();
        
        assertEquals(newQueueSize, resolved.queueSize());
    }
}
