package org.atlasapi.feeds.youview.resolution;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.nitro.NitroServiceIdResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;


public class UpdatedContentResolverTest {
    
    private static final Publisher PUBLISHER = Publisher.BBC_NITRO;
    private static final DateTime RECENT_TIMESTAMP = DateTime.now();
    private static final Content VIDEO_CONTENT = createItemWithMediaType(MediaType.VIDEO, "film");
    private static final Content AUDIO_CONTENT = createItemWithMediaType(MediaType.AUDIO, "audio");
    
    private LastUpdatedContentFinder contentFinder = Mockito.mock(LastUpdatedContentFinder.class);
    private NitroServiceIdResolver nitroServiceIdResolver = Mockito.mock(NitroServiceIdResolver.class);
    
    private final YouViewContentResolver resolver =
            new UpdatedContentResolver(contentFinder, PUBLISHER);

    @Before
    public void setup() {
        when(contentFinder.updatedSince(PUBLISHER, RECENT_TIMESTAMP)).thenReturn(Iterators.forArray(VIDEO_CONTENT, AUDIO_CONTENT));
        when(nitroServiceIdResolver.resolveMasterBrandId(VIDEO_CONTENT)).thenReturn(Optional.of("abc"));
    }

    @Test
    public void testFetchOfContentUpdatedSinceDateReturnsOnlyUpdatedVideoContent() {
        ImmutableSet<Content> content = ImmutableSet.copyOf(resolver.updatedSince(RECENT_TIMESTAMP));
        
        assertEquals(ImmutableSet.of(VIDEO_CONTENT), content);
    }
    
    private static Content createItemWithMediaType(MediaType audio, String uri) {
        Item item = new Item();
        item.setCanonicalUri(uri);
        item.setMediaType(audio);
        return item;
    }
}
