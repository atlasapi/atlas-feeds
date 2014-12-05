package org.atlasapi.feeds.youview.resolution;

import static org.atlasapi.feeds.youview.resolution.UpdatedContentResolver.START_OF_TIME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;


public class UpdatedContentResolverTest {
    
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final DateTime RECENT_TIMESTAMP = DateTime.now();
    private static final Content UPDATED_CONTENT = createItemWithMediaType(MediaType.VIDEO, "film");
    private static final Content NOT_UPDATED_CONTENT = createItemWithMediaType(MediaType.VIDEO, "another-film");
    private static final Content AUDIO_CONTENT = createItemWithMediaType(MediaType.AUDIO, "audio");
    
    private LastUpdatedContentFinder contentFinder = Mockito.mock(LastUpdatedContentFinder.class);
    
    private final YouViewContentResolver resolver = new UpdatedContentResolver(contentFinder, PUBLISHER);

    @Before
    public void setup() {
        when(contentFinder.updatedSince(PUBLISHER, RECENT_TIMESTAMP)).thenReturn(Iterators.forArray(UPDATED_CONTENT, AUDIO_CONTENT));
        when(contentFinder.updatedSince(PUBLISHER, START_OF_TIME)).thenReturn(Iterators.forArray(NOT_UPDATED_CONTENT, UPDATED_CONTENT, AUDIO_CONTENT));
    }

    @Test
    public void testFetchOfAllContentReturnsAllVideoContent() {
        ImmutableSet<Content> content = ImmutableSet.copyOf(resolver.allContent());
        
        assertEquals(ImmutableSet.of(NOT_UPDATED_CONTENT, UPDATED_CONTENT), content);
    }
    
    @Test
    public void testFetchOfContentUpdatedSinceDateReturnsOnlyUpdatedVideoContent() {
        ImmutableSet<Content> content = ImmutableSet.copyOf(resolver.updatedSince(RECENT_TIMESTAMP));
        
        assertEquals(ImmutableSet.of(UPDATED_CONTENT), content);
    }
    
    private static Content createItemWithMediaType(MediaType audio, String uri) {
        Item item = new Item();
        item.setCanonicalUri(uri);
        item.setMediaType(audio);
        return item;
    }
}
