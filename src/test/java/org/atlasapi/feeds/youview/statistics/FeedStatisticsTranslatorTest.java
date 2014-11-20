package org.atlasapi.feeds.youview.statistics;
import static org.atlasapi.feeds.youview.statistics.FeedStatisticsTranslator.QUEUE_SIZE_KEY;
import static org.atlasapi.feeds.youview.statistics.FeedStatisticsTranslator.UPDATE_LATENCY_KEY;
import static org.junit.Assert.assertEquals;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;
import com.mongodb.DBObject;



public class FeedStatisticsTranslatorTest {
    
    private static final Duration LATENCY = Duration.standardDays(1);
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final int QUEUE_SIZE = 100;

    private Clock clock = new TimeMachine();
    private final FeedStatisticsTranslator translator = new FeedStatisticsTranslator(clock);

    @Test
    public void testTranslationToDBObject() {
        FeedStatistics feedStats = createFeedStats();
        
        DBObject dbObject = translator.toDBObject(feedStats);
        
        assertEquals(PUBLISHER.name(), dbObject.get(MongoConstants.ID));
        assertEquals(QUEUE_SIZE, dbObject.get(QUEUE_SIZE_KEY));
        assertEquals(LATENCY.toString(), dbObject.get(UPDATE_LATENCY_KEY));
    }

    @Test
    public void testTranslationFromDBObject() {
        FeedStatistics feedStats = createFeedStats();
        
        FeedStatistics translated = translator.fromDBObject(translator.toDBObject(feedStats));
        
        assertEquals(feedStats.publisher(), translated.publisher());
        assertEquals(feedStats.queueSize(), translated.queueSize());
        assertEquals(feedStats.updateLatency(), translated.updateLatency());
        // can't make assertion about uptime, as is generated at resolution time
    }

    private FeedStatistics createFeedStats() {
        return new FeedStatistics(PUBLISHER, QUEUE_SIZE, LATENCY, DateTime.now());
    }

}
