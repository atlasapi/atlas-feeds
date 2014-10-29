package org.atlasapi.feeds.youview;

import static org.junit.Assert.*;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

// TODO there's a few edge cases still to test here
public class ContentHierarchyExtractorTest {
    
    private ContentResolver contentResolver = Mockito.mock(ContentResolver.class);
    private final ContentHierarchyExtractor hierarchy = new ContentResolvingContentHierarchyExtractor(contentResolver );

    @Test
    public void testResolvingFirstItemForASeries() {
        Item item = createItem();
        Series series = createSeries(Optional.of(item));
        
        mockContentResolutionFor(item);
        
        Item resolved = hierarchy.lastItemFrom(series);
        
        assertEquals(item, resolved);
    }

    @Test
    public void testResolvingFirstItemForABrand() {
        Item item = createItem();
        Brand brand = createBrand(Optional.of(item));
        
        mockContentResolutionFor(item);
        
        Item resolved = hierarchy.lastItemFrom(brand);
        
        assertEquals(item, resolved);
    }


    @Test
    public void testResolvingBrandForASeries() {
        Series series = createSeries(Optional.<Item>absent());
        Brand brand = createBrand(Optional.<Item>absent());
        series.setParentRef(ParentRef.parentRefFrom(brand));
        
        mockContentResolutionFor(brand);
        
        Brand resolved = hierarchy.brandFor(series).get();
        
        assertEquals(brand, resolved);
    }

    @Test
    public void testResolvingSeriesForAnItem() {
        Item item = createItem();
        Series series = createSeries(Optional.<Item>absent());
        item.setParentRef(ParentRef.parentRefFrom(series));
        
        mockContentResolutionFor(series);
        
        Series resolved = hierarchy.seriesFor(item).get();
        
        assertEquals(series, resolved);
    }

    @Test
    public void testResolvingBrandForAnItem() {
        Item item = createItem();
        Brand brand = createBrand(Optional.of(item));
        item.setContainer(brand);
        
        mockContentResolutionFor(brand);
        
        Brand resolved = hierarchy.brandFor(item).get();
        
        assertEquals(brand, resolved);
    }

    private Brand createBrand(Optional<Item> item) {
        Brand brand = new Brand("brand", "curie", Publisher.METABROADCAST);
        if (item.isPresent()) {
            brand.setChildRefs(Iterables.transform(Sets.newHashSet(item.get()), Item.TO_CHILD_REF));
        }
        return brand;
    }
    
    private Series createSeries(Optional<Item> item) {
        Series series = new Series("series", "curie", Publisher.METABROADCAST);
        if (item.isPresent()) {
            series.setChildRefs(Iterables.transform(Sets.newHashSet(item.get()), Item.TO_CHILD_REF));
        }
        return series;
    }
    
    private Item createItem() {
        return new Item("item", "curie", Publisher.METABROADCAST);
    }

    private void mockContentResolutionFor(Content content) {
        Mockito.when(contentResolver.findByCanonicalUris(ImmutableList.of(content.getCanonicalUri())))
                .thenReturn(resolvedContentFrom(content));
    }
    
    private ResolvedContent resolvedContentFrom(Content content) {
        return ResolvedContent.builder()
                .put(content.getCanonicalUri(), content)
                .build();
    }
}
