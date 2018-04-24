package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.SeriesRef;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkNotNull;


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
        //The repId service might have changed the ID of the brandRef we have here, but not the uri.
        //As we now resolved by uri we will get back the original item, but we really need to retain
        //the id of the repId for this brand. So will put the Brand back in an inconsistent state
        //by replacing the id with the one that we had. For Nitro that should be the same so no
        //actual change for amazon it should keep the repId.
        if (brandRef.getId() != null && item.getPublisher().equals(Publisher.AMAZON_UNBOX)) {
            brand.setId(brandRef.getId());
        }

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
        //The repId service might have changed the ID of the seriesRef we have here,but not the uri.
        //As we now resolved by uri we will get back the original item, but we really need to retain
        //the id of the repId for this series. So will put the Series back in an inconsistent state
        //by replacing the id with the one that we had. For Nitro that should be the same so no
        //actual change for amazon it should keep the repId.
        if (seriesRef.getId() != null && item.getPublisher().equals(Publisher.AMAZON_UNBOX)) {
            identified.setId(seriesRef.getId());
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
        //The repId service might have changed the ID of the brandRef we have here, but not the uri.
        //As we now resolved by uri we will get back the original item, but we really need to retain
        //the id of the repId for this brand. So will put the Brand back in an inconsistent state
        //by replacing the id with the one that we had. For Nitro that should be the same so no
        //actual change for amazon it should keep the repId.
        if (brandRef.getId() != null && series.getPublisher().equals(Publisher.AMAZON_UNBOX)) {
            brand.setId(brandRef.getId());
        }
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

    @Override
    public Iterable<Series> seriesFor(Brand brand) {
        Iterable<String> seriesUris = Iterables.transform(brand.getSeriesRefs(), SeriesRef.TO_URI);
        ResolvedContent resolved = contentResolver.findByCanonicalUris(seriesUris);
        return Iterables.filter(resolved.asResolvedMap().values(), Series.class);
    }

    @Override
    public Iterable<Item> itemsFor(Series series) {
        Iterable<String> itemUris = Iterables.transform(series.getChildRefs(), ChildRef.TO_URI);
        ResolvedContent resolved = contentResolver.findByCanonicalUris(itemUris);
        return Iterables.filter(resolved.asResolvedMap().values(), Item.class);
    }

}
