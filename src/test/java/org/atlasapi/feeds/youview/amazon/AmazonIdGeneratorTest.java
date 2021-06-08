package org.atlasapi.feeds.youview.amazon;

import com.google.common.collect.ImmutableList;
import org.atlasapi.TestsWithConfiguration;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Quality;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class AmazonIdGeneratorTest extends TestsWithConfiguration{

    private final IdGenerator generator = new AmazonIdGenerator();
    
    @Test
    public void testContentCridGeneration() {
        String contentCrid = generator.generateContentCrid(createItemWithId(12045L));
        
        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:wtf", contentCrid);
    }

    @Test
    public void testVersionCridGeneration() {
        String versionCrid = generator.generateVersionCrid(createItemWithId(12045L), createVersion());

        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:wtf:version", versionCrid);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBroadcastImiGeneration() {
        generator.generateBroadcastImi("serviceId", createBroadcast());
    }

    @Test
    public void testOnDemandImiGeneration() {
        String onDemandImi = generator.generateOnDemandImi(createItemWithId(12045L), createVersion(), createEncoding(), createLocations());
        
        assertEquals("imi:stage-metabroadcast.com/v3.amazon.co.uk:content:wtf:ondemand:SD", onDemandImi);
    }

    private Item createItemWithId(Long id) {
        Film film = new Film("http://v3.amazon.co.uk/amzn1.dv.gti.e2b8b2a4-94c7-a622-aa95-1af60936b0cd:GB", "curie", Publisher.AMAZON_V3);
        film.setId(id);
        return film;
    }

    private Version createVersion() {
        Version version = new Version();
        version.setCanonicalUri("crid://v3.amazon.co.uk/exec/obidos/ASIN/SOMELOCATIONID");
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
        location.setCanonicalUri("https://www.amazon.co.uk/gp/video/detail?gti=amzn1.dv.gti.e2b8b2a4-94c7-a622-aa95-1af60936b0cd");
        return ImmutableList.of(location);
    }
}
