package org.atlasapi.copying;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.atlasapi.beans.JsonTranslator;
import org.atlasapi.media.entity.simple.Item;
import org.atlasapi.media.entity.simple.Playlist;
import org.atlasapi.media.entity.testing.BroadcastTestDataBuilder;
import org.atlasapi.media.entity.testing.ItemTestDataBuilder;
import org.atlasapi.media.entity.testing.LocationTestDataBuilder;
import org.atlasapi.media.entity.testing.PlaylistTestDataBuilder;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;


public class AtlasContentCopyTest {
    

    @Test
    public void testCopyItem() throws Exception {
        JsonTranslator translator = new JsonTranslator();
        
        Item item = ItemTestDataBuilder.item()
            .withLocations(LocationTestDataBuilder.location().build())
            .withBroadcasts(BroadcastTestDataBuilder.broadcast().build())
            .withTags("tag1", "tag2")
            .withContainedIn("playlist1")
            .withClips(ItemTestDataBuilder.item().build(), ItemTestDataBuilder.item().build())
            .withSameAs("item2", "item3")
            .withAliases("item4")
            .build();
        
        ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream();
        
        translator.writeTo(ImmutableList.<Object>of(item), outputStream1);
        
        String itemOriginalString = outputStream1.toString(Charsets.UTF_8.name());
        outputStream1.close();
        
        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        
        translator.writeTo(ImmutableList.<Object>of(item.copy()), outputStream2);
        
        String itemCopyString = outputStream2.toString(Charsets.UTF_8.name());
        outputStream2.close();
        
        assertEquals(itemOriginalString, itemCopyString);
    }
    
    @Test 
    public void testCopyPlaylist() throws Exception {
        JsonTranslator translator = new JsonTranslator();
        
        Item item1 = ItemTestDataBuilder.item()
        .withLocations(LocationTestDataBuilder.location().build())
        .withBroadcasts(BroadcastTestDataBuilder.broadcast().build())
        .withTags("tag1", "tag2")
        .withContainedIn("playlist1")
        .withClips(ItemTestDataBuilder.item().build(), ItemTestDataBuilder.item().build())
        .withSameAs("item2", "item3")
        .withAliases("item4")
        .build();
        
        Item item2 = ItemTestDataBuilder.item()
        .withLocations(LocationTestDataBuilder.location().build())
        .withBroadcasts(BroadcastTestDataBuilder.broadcast().build())
        .withTags("tag1", "tag2")
        .withContainedIn("playlist1")
        .withClips(ItemTestDataBuilder.item().build(), ItemTestDataBuilder.item().build())
        .withSameAs("item2", "item3")
        .withAliases("item4")
        .build();
        
        Playlist playlist = PlaylistTestDataBuilder.playlist()
            .withItems(item1, item2)
            .withPlaylists(PlaylistTestDataBuilder.playlist().build())
            .build();
        
        ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream();
        
        translator.writeTo(ImmutableList.<Object>of(playlist), outputStream1);
        
        String playlistOriginalString = outputStream1.toString(Charsets.UTF_8.name());
        outputStream1.close();
        
        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        
        translator.writeTo(ImmutableList.<Object>of(playlist.copy()), outputStream2);
        
        String playlistCopyString = outputStream2.toString(Charsets.UTF_8.name());
        outputStream2.close();
        
        System.out.println(playlistOriginalString);
        System.out.println(playlistCopyString);
        
        assertEquals(playlistOriginalString, playlistCopyString);
    }
}
