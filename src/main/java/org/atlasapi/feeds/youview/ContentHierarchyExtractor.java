package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;

import com.google.common.base.Optional;


public interface ContentHierarchyExtractor {

    Optional<Brand> brandFor(Item item);
    Optional<Series> seriesFor(Item item);
    Optional<Brand> brandFor(Series series);
    Item firstItemFrom(Series series);
    Item firstItemFrom(Brand brand);
}
