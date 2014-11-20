package org.atlasapi.feeds.youview.statistics;
import static org.junit.Assert.assertEquals;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;



public class MongoFeedStatisticsStoreTest {
    
    private static final Duration LATENCY = Duration.standardDays(1);
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final int QUEUE_SIZE = 100;

    private Clock clock = new TimeMachine();
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    
    private final MongoFeedStatisticsStore store = new MongoFeedStatisticsStore(mongo, clock);

    @Test
    public void testSaveAndResolutionOfStats() {
        FeedStatistics feedStats = createFeedStats();
        
        store.save(feedStats);
        
        FeedStatistics resolved = store.resolveFor(PUBLISHER).get();
        
        assertEquals(feedStats.publisher(), resolved.publisher());
        assertEquals(feedStats.queueSize(), resolved.queueSize());
        assertEquals(feedStats.updateLatency(), resolved.updateLatency());
    }

    @Test
    public void testUpdateOfQueueSize() {
        FeedStatistics feedStats = createFeedStats();
        
        store.save(feedStats);
        
        int newQueueSize = QUEUE_SIZE + 50;
        store.updateQueueSize(PUBLISHER, newQueueSize);
        
        FeedStatistics resolved = store.resolveFor(PUBLISHER).get();
        
        assertEquals(newQueueSize, resolved.queueSize());
    }

    @Test
    public void testUpdateOfUpdateLatency() {
        FeedStatistics feedStats = createFeedStats();
        
        store.save(feedStats);
        
        Duration newLatency = LATENCY.plus(Duration.standardHours(10));
        store.updateAverageLatency(PUBLISHER, newLatency);
        
        FeedStatistics resolved = store.resolveFor(PUBLISHER).get();
        
        assertEquals(newLatency, resolved.updateLatency());
    }

    private FeedStatistics createFeedStats() {
        return new FeedStatistics(PUBLISHER, QUEUE_SIZE, LATENCY, DateTime.now());
    }

}
