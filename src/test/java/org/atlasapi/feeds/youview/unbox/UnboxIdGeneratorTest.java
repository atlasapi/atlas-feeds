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
        
        assertEquals("crid://amazon.com/stage-metabroadcast.com/content/wtf", contentCrid);
    }

    @Test
    public void testVersionCridGeneration() {
        String versionCrid = generator.generateVersionCrid(createItemWithId(12045L), createVersion());
        
        assertEquals("crid://amazon.com/stage-metabroadcast.com/content/wtf/version", versionCrid);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBroadcastImiGeneration() {
        generator.generateBroadcastImi("serviceId", createBroadcast());
    }

    @Test
    public void testOnDemandImiGeneration() {
        String onDemandImi = generator.generateOnDemandImi(createItemWithId(12045L), createVersion(), createEncoding(), createLocation());
        
        assertEquals("crid://amazon.com/stage-metabroadcast.com/content/wtf/ondemand/SD", onDemandImi);
    }

    private Item createItemWithId(Long id) {
        Film film = new Film("http://unbox.amazon.co.uk/films/123456", "curie", Publisher.LOVEFILM);
        film.setId(id);
        return film;
    }

    private Version createVersion() {
        Version version = new Version();
        version.setCanonicalUri("crid://amazon.com/exec/obidos/ASIN/SOMELOCATIONID");
        version.setId(12045L);
        return version;
    }
    private Broadcast createBroadcast() {
        return new Broadcast("http://bbc.co.uk/services/bbcone", DateTime.now(), Duration.standardMinutes(30));
    }

    private Encoding createEncoding() {
        Encoding encoding = new Encoding();
        encoding.setCanonicalUri("encodingUri");
        return encoding;
    }
    
    private Location createLocation() {

        Location location = new Location();
        location.setCanonicalUri("http://www.amazon.co.uk/gp/product/B072NZYNMT/PAY_TO_RENT");
        return location;
    }

}
