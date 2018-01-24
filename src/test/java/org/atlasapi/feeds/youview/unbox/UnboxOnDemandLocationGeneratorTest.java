package org.atlasapi.feeds.youview.unbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.math.BigInteger;
import java.util.Set;

import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import tva.metadata._2010.AVAttributesType;
import tva.metadata._2010.AudioAttributesType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.VideoAttributesType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;

public class UnboxOnDemandLocationGeneratorTest {
    
    private static final Function<GenreType, String> GENRE_TO_HREF = new Function<GenreType, String>() {
        @Override
        public String apply(GenreType input) {
            return input.getHref();
        }
    };
    
    private static final Function<GenreType, String> GENRE_TO_TYPE = new Function<GenreType, String>() {
        @Override
        public String apply(GenreType input) {
            return input.getType();
        }
    };

    private IdGenerator idGenerator = new UnboxIdGenerator();
    
    private final OnDemandLocationGenerator generator = new UnboxOnDemandLocationGenerator(idGenerator);

    @Test
    public void testNonPublisherSpecificFields() {
        Location location = createLocation();
        location.setCanonicalUri("unbox.amazon.co.uk/SOMELOCATIONID");
        Encoding encoding = createEncoding(location);
        Version version = createVersion(encoding);
        version.setCanonicalUri("unbox.amazon.co.uk/SOMEVERSIONID");
        Film film = createFilm(version);
        film.setId(1L);
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(film, version, encoding, location);
        String onDemandImi = "onDemandImi";
        
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, onDemandImi);

        assertEquals("P0DT1H30M0.000S", onDemand.getPublishedDuration().toString());
        assertEquals("2012-07-03T00:00:00Z", onDemand.getStartOfAvailability().toString());
        assertEquals("2013-07-17T00:00:00Z", onDemand.getEndOfAvailability().toString());
        assertFalse(onDemand.getFree().isValue());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        
        Set<String> hrefs = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_HREF));
        Set<String> types = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_TYPE));
                
        assertEquals("other", Iterables.getOnlyElement(types));
        
        Set<String> expected = ImmutableSet.of(
                UnboxOnDemandLocationGenerator.YOUVIEW_GENRE_MEDIA_AVAILABLE, //everything has it
                UnboxOnDemandLocationGenerator.YOUVIEW_ENTITLEMENT_PAY_TO_BUY
        ); //the test location has it
        
        assertEquals(expected, hrefs);

        AVAttributesType avAttributes = instanceDesc.getAVAttributes();
        AudioAttributesType audioAttrs = Iterables.getOnlyElement(avAttributes.getAudioAttributes());
        
        assertEquals("urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3", audioAttrs.getMixType().getHref());
        
        VideoAttributesType videoAttrs = avAttributes.getVideoAttributes();
        
        assertEquals(Integer.valueOf(1280), videoAttrs.getHorizontalSize());
        assertEquals(Integer.valueOf(720), videoAttrs.getVerticalSize());
        assertEquals("16:9", Iterables.getOnlyElement(videoAttrs.getAspectRatio()).getValue());

        assertEquals(BigInteger.valueOf(3308), avAttributes.getBitRate().getValue());
        assertFalse(avAttributes.getBitRate().isVariable());
        
        UniqueIDType otherId = Iterables.getOnlyElement(instanceDesc.getOtherIdentifier());
        assertEquals("filmAsin", otherId.getValue());
    }
    
    @Test
    public void testUnboxSpecificFields() {
        Location location = createLocation();
        location.setCanonicalUri("unbox.amazon.co.uk/SOMELOCATIONID/SUBSCRIPTION");
        Encoding encoding = createEncoding(location);
        Version version = createVersion(encoding);
        version.setCanonicalUri("unbox.amazon.co.uk/SOMELOCATIONID");
        Film film = createFilm(version);
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(film, version, encoding, location);
        String onDemandImi = "onDemandImi";
        
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, onDemandImi);
        
        assertEquals("http://amazon.com/services/on_demand/primevideo", onDemand.getServiceIDRef());
        assertEquals("crid://amazon.com/exec/obidos/ASIN/SOMELOCATIONID_version", onDemand.getProgram().getCrid());
        assertEquals("imi:amazon.com/SOMELOCATIONID", onDemand.getInstanceMetadataId());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        UniqueIDType otherId = Iterables.getOnlyElement(instanceDesc.getOtherIdentifier());
        assertEquals("deep_linking_id.amazon.com", otherId.getAuthority());
    }

    private Film createFilm(Version version) {
        Film film = new Film();
        
        film.setCanonicalUri("http://unbox.amazon.co.uk/movies/177221");
        film.setGenres(ImmutableList.of("http://unbox.amazon.co.uk/genres/comedy"));
        film.setPublisher(Publisher.AMAZON_UNBOX);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.addAlias(new Alias("gb:amazon:asin", "filmAsin"));
        film.addVersion(version);
        
        return film;
    }

    private Version createVersion(Encoding encoding) {
        Version version = new Version();
        
        version.addManifestedAs(encoding);
        version.setDuration(Duration.standardMinutes(90));
        
        return version;
    }

    private Encoding createEncoding(Location location) {
        Encoding encoding = new Encoding();
        
        encoding.setVideoHorizontalSize(1280);
        encoding.setVideoVerticalSize(720);
        encoding.setVideoAspectRatio("16:9");
        encoding.setBitRate(3308);
        encoding.addAvailableAt(location);
        
        return encoding;
    }

    private Location createLocation() {
        Location location = new Location();
        
        Policy policy = new Policy();
        policy.setRevenueContract(Policy.RevenueContract.PAY_TO_BUY);
        policy.setAvailabilityStart(new DateTime(2012, 7, 3, 0, 0, 0, DateTimeZone.UTC));
        policy.setAvailabilityEnd(new DateTime(2013, 7, 17, 0, 0, 0, DateTimeZone.UTC));
        
        location.setPolicy(policy);
        
        return location;
    }
}
