package org.atlasapi.feeds.youview.persistence;

import org.joda.time.DateTime;

import com.google.common.base.Optional;

public interface YouViewLastUpdatedStore {

    public Optional<DateTime> getLastUpdated();
    public void setLastUpdated(DateTime lastUpdated);
}
