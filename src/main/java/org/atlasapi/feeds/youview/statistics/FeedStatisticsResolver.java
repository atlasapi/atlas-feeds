package org.atlasapi.feeds.youview.statistics;

import java.time.Duration;

import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Optional;

public interface FeedStatisticsResolver {

    Optional<FeedStatistics> resolveFor(Publisher publisher, Duration duration);
}
