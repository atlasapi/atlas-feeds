package org.atlasapi.feeds.youview.resolution;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;


public class FullHierarchyResolvingContentResolverTest {
    
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final DateTime TIMESTAMP = DateTime.now();
    
    private YouViewContentResolver delegate = Mockito.mock(YouViewContentResolver.class);
    private ContentHierarchyExtractor hierarchyExtractor = Mockito.mock(ContentHierarchyExtractor.class);
    
    private final YouViewContentResolver resolver = new FullHierarchyResolvingContentResolver(delegate, hierarchyExtractor);

    @Test
    public void testBrandIsAlwaysPassedThroughAndNothingAdded() {
        Brand brand = new Brand("brand", "curie", PUBLISHER);
        
        alwaysReturnContentFromDelegate(brand);
        
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(brand);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testSeriesWithBrandResolvesBrandAndReturnsBoth() {
        Series series = new Series("series", "curie", PUBLISHER);
        Brand brand = new Brand("brand", "curie", PUBLISHER);
        series.setParent(brand);
        
        when(hierarchyExtractor.brandFor(series)).thenReturn(Optional.of(brand));
        alwaysReturnContentFromDelegate(series);
        
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(series, brand);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testSeriesWithoutBrandReturnsJustSeries() {
        Series series = new Series("series", "curie", PUBLISHER);
        
        when(hierarchyExtractor.brandFor(series)).thenReturn(Optional.<Brand>absent());
        alwaysReturnContentFromDelegate(series);
        
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(series);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testItemWithBrandResolvesBrandAndReturnsBoth() {
        Item item = new Item("item", "curie", PUBLISHER);
        Brand brand = new Brand("brand", "curie", PUBLISHER);
        item.setContainer(brand);
        
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.of(brand));
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.<Series>absent());
        alwaysReturnContentFromDelegate(item);
        
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(item, brand);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testItemWithSeriesResolvesSeriesAndReturnsBoth() {
        Item item = new Item("item", "curie", PUBLISHER);
        Series series = new Series("series", "curie", PUBLISHER);
        item.setContainer(series);
        
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.<Brand>absent());
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.of(series));
        alwaysReturnContentFromDelegate(item);
        
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(item, series);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testItemWithBrandAndSeriesResolvesBothAndReturnsFullHierarchy() {
        Episode episode = new Episode("episode", "curie", PUBLISHER);
        Brand brand = new Brand("brand", "curie", PUBLISHER);
        Series series = new Series("series", "curie", PUBLISHER);
        episode.setContainer(brand);
        episode.setSeries(series);
        
        when(hierarchyExtractor.brandFor(episode)).thenReturn(Optional.of(brand));
        when(hierarchyExtractor.seriesFor(episode)).thenReturn(Optional.of(series));
        alwaysReturnContentFromDelegate(episode);
        
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(episode, series, brand);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testItemWithNoParentsReturnsJustItem() {
        Item item = new Item("item", "curie", PUBLISHER);
        
        when(hierarchyExtractor.brandFor(item)).thenReturn(Optional.<Brand>absent());
        when(hierarchyExtractor.seriesFor(item)).thenReturn(Optional.<Series>absent());
        alwaysReturnContentFromDelegate(item);
        
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(item);
        assertEquals(expected, updatedContent);
    }

    private void alwaysReturnContentFromDelegate(Content content) {
        when(delegate.updatedSince(any(DateTime.class))).thenReturn(Iterators.forArray(content));
    }
}
