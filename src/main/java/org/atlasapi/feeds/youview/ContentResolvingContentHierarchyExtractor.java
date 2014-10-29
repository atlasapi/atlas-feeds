package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;


public class ContentResolvingContentHierarchyExtractor implements ContentHierarchyExtractor {
    
    private final ContentResolver contentResolver;
    
    public ContentResolvingContentHierarchyExtractor(ContentResolver contentResolver) {
        this.contentResolver = checkNotNull(contentResolver);
    }

    @Override
    public Optional<Brand> brandFor(Item item) {
        ParentRef brandRef = item.getContainer();
        if (brandRef == null) {
            return Optional.absent();
        }
        ResolvedContent resolved = contentResolver.findByCanonicalUris(ImmutableList.of(brandRef.getUri()));
        Identified identified = resolved.asResolvedMap().get(brandRef.getUri());
        if (!(identified instanceof Brand)) {
            return Optional.absent();
        }
        Brand brand = (Brand) identified;
        return Optional.fromNullable(brand);
    }

    @Override
    public Optional<Series> seriesFor(Item item) {
        ParentRef seriesRef;
        if (item instanceof Episode) {
            Episode episode = (Episode) item;
            seriesRef = episode.getSeriesRef();
            if (seriesRef == null) {
                seriesRef = episode.getContainer();
                if (seriesRef == null) {
                    return Optional.absent();
                }
            }
        } else {
            seriesRef = item.getContainer();
            if (seriesRef == null) {
                return Optional.absent();
            }
        }
        ResolvedContent resolved = contentResolver.findByCanonicalUris(ImmutableList.of(seriesRef.getUri()));
        Identified identified = resolved.asResolvedMap().get(seriesRef.getUri());
        if (!(identified instanceof Series)) {
            return Optional.absent();
        }
        Series series = (Series) identified;
        return Optional.fromNullable(series);
    }

    @Override
    public Optional<Brand> brandFor(Series series) {
        ParentRef brandRef = series.getParent();
        if (brandRef == null) {
            return Optional.absent();
        }
        ResolvedContent resolved = contentResolver.findByCanonicalUris(ImmutableList.of(brandRef.getUri()));
        Brand brand = (Brand) resolved.asResolvedMap().get(brandRef.getUri());
        return Optional.fromNullable(brand);
    }

    @Override
    public Item lastItemFrom(Series series) {
        ChildRef last = Iterables.getLast(series.getChildRefs());
        ResolvedContent resolved = contentResolver.findByCanonicalUris(ImmutableList.of(last.getUri()));
        return (Item) resolved.asResolvedMap().get(last.getUri());
    }

    @Override
    public Item lastItemFrom(Brand brand) {
        ChildRef last = Iterables.getLast(brand.getChildRefs());
        ResolvedContent resolved = contentResolver.findByCanonicalUris(ImmutableList.of(last.getUri()));
        return (Item) resolved.asResolvedMap().get(last.getUri()); 
    }

}
