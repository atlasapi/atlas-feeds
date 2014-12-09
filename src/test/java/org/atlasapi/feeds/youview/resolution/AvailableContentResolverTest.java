package org.atlasapi.feeds.youview.resolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;


public class AvailableContentResolverTest {
    
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final DateTime TIMESTAMP = DateTime.now();
    
    private YouViewContentResolver delegate = Mockito.mock(YouViewContentResolver.class);
    
    private final YouViewContentResolver resolver = new AvailableContentResolver(delegate);

    @Test
    public void testBrandIsAlwaysPassedThroughFromDelegate() {
        Brand brand = new Brand("brand", "curie", PUBLISHER);
        
        alwaysReturnContentFromDelegate(brand);
        
        ImmutableSet<Content> allContent = ImmutableSet.copyOf(resolver.allContent());
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(brand);
        assertEquals(expected, allContent);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testSeriesIsAlwaysPassedThroughFromDelegate() {
        Series series = new Series("series", "curie", PUBLISHER);
        
        alwaysReturnContentFromDelegate(series);
        
        ImmutableSet<Content> allContent = ImmutableSet.copyOf(resolver.allContent());
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(series);
        assertEquals(expected, allContent);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testItemWithNoAvailabilitiesIsNotPassedThroughFromDelegate() {
        Film film = new Film("item", "curie", PUBLISHER);
        
        alwaysReturnContentFromDelegate(film);
        
        ImmutableSet<Content> allContent = ImmutableSet.copyOf(resolver.allContent());
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        assertTrue("Content has no availability, so shouldn't be passed through", allContent.isEmpty());
        assertTrue("Content has no availability, so shouldn't be passed through", updatedContent.isEmpty());
    }

    @Test
    public void testItemWithNoCurrentAvailabilitiesIsNotPassedThroughFromDelegate() {
        Film film = new Film("item", "curie", PUBLISHER);
        film.addVersion(createVersionWithoutAvailability());
        
        alwaysReturnContentFromDelegate(film);
        
        ImmutableSet<Content> allContent = ImmutableSet.copyOf(resolver.allContent());
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        assertTrue("Content has no current availability, so shouldn't be passed through", allContent.isEmpty());
        assertTrue("Content has no current availability, so shouldn't be passed through", updatedContent.isEmpty());
    }

    @Test
    public void testItemWithCurrentAvailabilitiesIsAlwaysPassedThroughFromDelegate() {
        Film film = new Film("item", "curie", PUBLISHER);
        film.addVersion(createVersionWithAvailability());
        
        alwaysReturnContentFromDelegate(film);
        
        ImmutableSet<Content> allContent = ImmutableSet.copyOf(resolver.allContent());
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(film);
        assertEquals(expected, allContent);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testItemWithSomeCurrentAndSomeNonCurrentAvailabilitiesIsAlwaysPassedThroughFromDelegate() {
        Film film = new Film("item", "curie", PUBLISHER);
        film.addVersion(createVersionWithAvailability());
        film.addVersion(createVersionWithoutAvailability());
        
        alwaysReturnContentFromDelegate(film);
        
        ImmutableSet<Content> allContent = ImmutableSet.copyOf(resolver.allContent());
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(TIMESTAMP));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(film);
        assertEquals(expected, allContent);
        assertEquals(expected, updatedContent);
    }

    private void alwaysReturnContentFromDelegate(Content content) {
        when(delegate.allContent()).thenReturn(Iterators.forArray(content));
        when(delegate.updatedSince(any(DateTime.class))).thenReturn(Iterators.forArray(content));
    }

    private Version createVersionWithAvailability() {
        return createWithAvailability(true);
    }

    private Version createVersionWithoutAvailability() {
        return createWithAvailability(false);
    }

    private Version createWithAvailability(boolean available) {
        Version version = new Version();
        
        Encoding encoding = new Encoding();
        
        Location location = new Location();
        
        location.setAvailable(available);
        
        encoding.setAvailableAt(ImmutableSet.of(location));
        
        version.setManifestedAs(ImmutableSet.of(encoding));
        
        return version;
    }
}
