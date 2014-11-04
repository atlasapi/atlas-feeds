package org.atlasapi.feeds.youview.statistics;
import static org.junit.Assert.assertEquals;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;



public class MockDataFeedStatisticsResolverTest {
    
    private static final Publisher publisher = Publisher.METABROADCAST;
    
    private FeedStatistics mockedStats = createFeedStats();
    private final MockDataFeedStatisticsResolver mockResolver = new MockDataFeedStatisticsResolver(mockedStats);

    // this test is a bit pointless
    @Test
    public void testResolverAlwaysReturnsInputStats() {
        for (int i = 0; i < 10; i++) {
            FeedStatistics stats = mockResolver.resolveFor(publisher).get();
            assertEquals(mockedStats, stats);
        }
    }

    private FeedStatistics createFeedStats() {
        return new FeedStatistics(publisher, 100, Duration.standardDays(1), DateTime.now());
    }

}
