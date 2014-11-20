package org.atlasapi.feeds.youview.statistics;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Optional;


public class MockDataFeedStatisticsResolver implements FeedStatisticsResolver {
    
    private final FeedStatistics mockedStats;
    
    public MockDataFeedStatisticsResolver(FeedStatistics mockedStats) {
        this.mockedStats = checkNotNull(mockedStats);
    }

    @Override
    public Optional<FeedStatistics> resolveFor(Publisher publisher) {
        return Optional.of(mockedStats);
    }

}
