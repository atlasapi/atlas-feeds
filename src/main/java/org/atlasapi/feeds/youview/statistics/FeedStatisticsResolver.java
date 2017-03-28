package org.atlasapi.feeds.youview.statistics;

import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Optional;
import org.joda.time.Duration;

public interface FeedStatisticsResolver {

    Optional<FeedStatistics> resolveFor(Publisher publisher, Duration duration);
}
