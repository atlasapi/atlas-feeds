package org.atlasapi.feeds.youview.persistence;

import org.joda.time.DateTime;

public interface YouViewLastUpdatedStore {

    public DateTime getLastUpdated();
    public void setLastUpdated(DateTime lastUpdated);
}
