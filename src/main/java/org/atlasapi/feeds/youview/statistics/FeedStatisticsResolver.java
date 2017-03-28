package org.atlasapi.feeds.youview.statistics;

import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Optional;
import org.joda.time.Period;

public interface FeedStatisticsResolver {

    Optional<FeedStatistics> resolveFor(Publisher publisher, Period timeBeforeNow);
}
