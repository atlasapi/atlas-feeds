package org.atlasapi.feeds.youview.statistics;

import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.MongoTaskStore;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.TimeMachine;


public class MongoFeedStatisticsStoreTest {
    
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private final TaskStore taskStore = new MongoTaskStore(mongo);
    private TimeMachine clock = new TimeMachine();

    private final MongoFeedStatisticsStore store = new MongoFeedStatisticsStore(mongo, taskStore, clock, YOUVIEW);

    @Test
    public void testFetchOfAbsentDayAndPublisherReturnsZeroLatencyAndSize() {
        Optional<FeedStatistics> statsOptional = store.resolveFor(Publisher.BT_BLACKOUT);
        FeedStatistics stats = statsOptional.get();
        assertEquals(0, stats.queueSize());
        assertEquals(Duration.ZERO, stats.updateLatency());
    }

    @Test
    public void queueSizeIsReportedCorrectly() {
        taskStore.save(getTask(42L));
        taskStore.save(getTask(43L));
        assertEquals(2, store.resolveFor(PUBLISHER).get().queueSize());
    }

    @Test
    public void queueLatencyIsReportedCorrectly() {
        DateTime now = DateTime.now();
        DateTime created = now.minusDays(1);
        taskStore.save(getTask(44L, created));

        clock.jumpTo(now);

        assertEquals(Duration.standardDays(1), store.resolveFor(PUBLISHER).get().updateLatency());
    }

    private Task getTask(Long id, DateTime created) {
        return Task.builder()
                .withId(id)
                .withCreated(DateTime.now())
                .withPublisher(PUBLISHER)
                .withAction(Action.UPDATE)
                .withDestination(new YouViewDestination("", TVAElementType.ONDEMAND, ""))
                .withStatus(Status.NEW)
                .withUploadTime(DateTime.now())
                .withCreated(created)
                .build();
    }

    private Task getTask(Long id) {
        return getTask(id, DateTime.now());
    }
}
