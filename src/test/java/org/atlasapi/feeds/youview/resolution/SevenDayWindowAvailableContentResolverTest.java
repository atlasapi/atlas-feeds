package org.atlasapi.feeds.youview.resolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class SevenDayWindowAvailableContentResolverTest {
    
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    
    private Clock clock = new TimeMachine();
    
    private YouViewContentResolver delegate = Mockito.mock(YouViewContentResolver.class);
    
    private final YouViewContentResolver resolver = new SevenDayWindowAvailableContentResolver(delegate);

    @Test
    public void testItemWithNoCurrentAvailabilitiesIsNotPassedThroughFromDelegate() {
        Film film = new Film("item", "curie", PUBLISHER);
        film.addVersion(createVersionWithAvailability(clock.now().minusDays(20), clock.now().minusDays(10)));
        
        alwaysReturnContentFromDelegate(film);
        
        ImmutableSet<Content> allContent = ImmutableSet.copyOf(resolver.allContent());
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(clock.now()));
        
        assertTrue("Content has no current availability, so shouldn't be passed through", allContent.isEmpty());
        assertTrue("Content has no current availability, so shouldn't be passed through", updatedContent.isEmpty());
    }

    @Test
    public void testItemWithCurrentAvailabilityThatStartsLessThanSevenDaysAgoIsAlwaysPassedThroughFromDelegate() {
        Film film = new Film("item", "curie", PUBLISHER);
        film.addVersion(createVersionWithAvailability(clock.now().minusDays(5), clock.now().plusDays(10)));
        
        alwaysReturnContentFromDelegate(film);
        
        ImmutableSet<Content> allContent = ImmutableSet.copyOf(resolver.allContent());
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(clock.now()));
        
        ImmutableSet<Content> expected = ImmutableSet.<Content>of(film);
        assertEquals(expected, allContent);
        assertEquals(expected, updatedContent);
    }

    @Test
    public void testItemWithCurrentAvailabilityThatStartsMoreThanSevenDaysAgoIsNotPassedThroughFromDelegate() {
        Film film = new Film("item", "curie", PUBLISHER);
        film.addVersion(createVersionWithAvailability(clock.now().minusDays(10), clock.now().plusDays(10)));
        
        alwaysReturnContentFromDelegate(film);

        ImmutableSet<Content> allContent = ImmutableSet.copyOf(resolver.allContent());
        ImmutableSet<Content> updatedContent = ImmutableSet.copyOf(resolver.updatedSince(clock.now()));
        
        assertTrue("Content has no current availability, so shouldn't be passed through", allContent.isEmpty());
        assertTrue("Content has no current availability, so shouldn't be passed through", updatedContent.isEmpty());
    }

    private void alwaysReturnContentFromDelegate(Content content) {
        when(delegate.allContent()).thenReturn(Iterators.forArray(content));
        when(delegate.updatedSince(any(DateTime.class))).thenReturn(Iterators.forArray(content));
    }

    private Version createVersionWithAvailability(DateTime start, DateTime end) {
        Version version = new Version();
        
        Encoding encoding = new Encoding();
        
        Location location = new Location();

        Policy policy = new Policy();
        
        policy.setAvailabilityStart(start);
        policy.setAvailabilityEnd(end);
        policy.setPlatform(Platform.YOUVIEW_IPLAYER);
        
        location.setPolicy(policy);
        
        encoding.setAvailableAt(ImmutableSet.of(location));
        
        version.setManifestedAs(ImmutableSet.of(encoding));
        
        return version;
    }
}
