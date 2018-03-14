package org.atlasapi.feeds.youview.unbox;

import java.util.List;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Quality;
import org.atlasapi.media.entity.Version;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;


public class AmazonIdGeneratorTest {

    private final IdGenerator generator = new AmazonIdGenerator();

    @BeforeClass
    public static void setUp() {
        System.setProperty("MBST_PLATFORM", "stage");
    }
    
    @Test
    public void testContentCridGeneration() {
        String contentCrid = generator.generateContentCrid(createItemWithId(12045L));
        
        assertEquals("crid://stage-metabroadcast.com/amazon.com:content:wtf", contentCrid);
    }

    @Test
    public void testVersionCridGeneration() {
        String versionCrid = generator.generateVersionCrid(createItemWithId(12045L), createVersion());
        
        assertEquals("crid://stage-metabroadcast.com/amazon.com:content:wtf:version", versionCrid);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBroadcastImiGeneration() {
        generator.generateBroadcastImi("serviceId", createBroadcast());
    }

    @Test
    public void testOnDemandImiGeneration() {
        String onDemandImi = generator.generateOnDemandImi(createItemWithId(12045L), createVersion(), createEncoding(), createLocations());
        
        assertEquals("imi:stage-metabroadcast.com/amazon.com:content:wtf:ondemand:SD", onDemandImi);
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
        encoding.setQuality(Quality.SD);
        encoding.setCanonicalUri("encodingUri");
        return encoding;
    }
    
    private List<Location> createLocations() {

        Location location = new Location();
        location.setCanonicalUri("http://www.amazon.co.uk/gp/product/B072NZYNMT/PAY_TO_RENT");
        return ImmutableList.of(location);
    }

}
