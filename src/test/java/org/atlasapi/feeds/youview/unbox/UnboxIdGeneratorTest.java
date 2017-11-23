package org.atlasapi.feeds.youview.unbox;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;


public class UnboxIdGeneratorTest {

    private final IdGenerator generator = new UnboxIdGenerator();
    
    @Test
    public void testContentCridGeneration() {
        String contentCrid = generator.generateContentCrid(createItemWithId(12045L));
        
        assertEquals("crid://stage-metabroadcast.com/content/wtf", contentCrid);
    }

    @Test
    public void testVersionCridGeneration() {
        String versionCrid = generator.generateVersionCrid(createItemWithId(12045L), createVersion());
        
        assertEquals("crid://stage-metabroadcast.com/content/wtf_version", versionCrid);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBroadcastImiGeneration() {
        generator.generateBroadcastImi("serviceId", createBroadcast());
    }

    @Test
    public void testOnDemandImiGeneration() {
        String onDemandImi = generator.generateOnDemandImi(createItemWithId(12045L), createVersion(), createEncoding(), createLocation());
        
        assertEquals("imi:amazon.com/123456", onDemandImi);
    }

    private Item createItemWithId(Long id) {
        Film film = new Film("http://unbox.amazon.co.uk/films/123456", "curie", Publisher.LOVEFILM);
        film.setId(id);
        return film;
    }

    private Version createVersion() {
        return new Version();
    }

    private Broadcast createBroadcast() {
        return new Broadcast("http://bbc.co.uk/services/bbcone", DateTime.now(), Duration.standardMinutes(30));
    }

    private Encoding createEncoding() {
        return new Encoding();
    }
    
    private Location createLocation() {
        return new Location();
    }

}
