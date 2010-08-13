package org.atlasapi.feeds.interlinking;

import java.util.List;

import org.atlasapi.media.entity.Brand;
import org.joda.time.DateTime;

public interface PlaylistToInterlinkFeed {

    public InterlinkFeed fromBrands(String id, String publisher, DateTime from, DateTime to, List<Brand> brands);
}