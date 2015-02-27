package org.atlasapi.feeds.youview.statistics;

import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class MongoFeedStatisticsStoreTest {
    
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private TaskStore taskStore = mock(TaskStore.class);
    private Clock clock = new TimeMachine();
    private Lock lock = new ReentrantLock();
    
    private final MongoFeedStatisticsStore store = new MongoFeedStatisticsStore(mongo, taskStore, clock, YOUVIEW, lock);
    
    @Before
    public void setup() {
        Task t = mock(Task.class);
        Mockito.when(taskStore.allTasks(YOUVIEW, Status.NEW)).thenReturn(ImmutableSet.of(t));
    }

    @Test
    public void testFetchOfAbsentDayAndPublisherReturnsAbsent() {
        Optional<FeedStatistics> stats = store.resolveFor(PUBLISHER);
        
        assertFalse("Feed stats should not be returned if no record exists for the provided publisher and day", stats.isPresent());
    }
    
    @Test
    public void testFetchOfDayWithSingleLatencyReturnsThatLatency() {
        Duration latency = Duration.standardHours(3);
        
        store.updateFeedStatistics(PUBLISHER, latency);
        FeedStatistics stats = store.resolveFor(PUBLISHER).get();
        
        assertEquals(latency, stats.updateLatency());
    }
    
    @Test
    public void testFetchOfDayWithMultipleLatenciesReturnsAverageOfWrittenLatencies() {
        Duration latency1 = Duration.standardHours(2);
        Duration latency2 = Duration.standardHours(4);
        
        store.updateFeedStatistics(PUBLISHER, latency1);
        store.updateFeedStatistics(PUBLISHER, latency2);
        
        FeedStatistics stats = store.resolveFor(PUBLISHER).get();
        
        assertEquals(Duration.standardHours(3), stats.updateLatency());
    }
}
