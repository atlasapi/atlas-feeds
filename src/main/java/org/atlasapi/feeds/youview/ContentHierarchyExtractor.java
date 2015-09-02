package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;

import com.google.common.base.Optional;


public interface ContentHierarchyExtractor {

    Optional<Brand> brandFor(Item item);
    Optional<Brand> brandFor(Series series);
    Optional<Series> seriesFor(Item item);
    Iterable<Series> seriesFor(Brand brand);
    Iterable<Item> itemsFor(Series series);
    Item lastItemFrom(Series series);
    Item lastItemFrom(Brand brand);
}
