package org.atlasapi.feeds.youview.persistence;

import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

import com.google.common.base.Optional;

public interface YouViewLastUpdatedStore {

    public Optional<DateTime> getLastUpdated(Publisher publisher);

    public Optional<DateTime> getLastRepIdChangesChecked(Publisher publisher);

    public void setLastUpdated(DateTime lastUpdated, Publisher publisher);

    public void setLastRepIdChecked(DateTime lastUpdated, Publisher publisher);
}
