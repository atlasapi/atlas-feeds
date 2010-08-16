package org.atlasapi.feeds.interlinking;

import java.util.List;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

public interface PlaylistToInterlinkFeed {

    public InterlinkFeed fromBrands(String id, Publisher publisher, DateTime from, DateTime to, List<Brand> brands);
}