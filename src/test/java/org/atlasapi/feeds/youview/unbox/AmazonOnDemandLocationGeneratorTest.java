package org.atlasapi.feeds.youview.unbox;

import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;

import org.atlasapi.TestsWithConfiguration;
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
import org.atlasapi.media.entity.Quality;
import org.atlasapi.media.entity.Version;

import com.metabroadcast.common.intl.Countries;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Ignore;
import org.junit.Test;
import tva.metadata._2010.AVAttributesType;
import tva.metadata._2010.AudioAttributesType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.VideoAttributesType;
import tva.mpeg7._2008.UniqueIDType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AmazonOnDemandLocationGeneratorTest extends TestsWithConfiguration {
    
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

    private IdGenerator idGenerator = new AmazonIdGenerator();
    
    private final OnDemandLocationGenerator generator = new AmazonOnDemandLocationGenerator(idGenerator);

    @Test
    public void testGenreTypesHaveNoDuplicated() {
        Location location1 = createLocationSub();
        location1.setCanonicalUri("unbox.amazon.co.uk/SOME_ASIN/SUBSCRIPTION");
        Location location2 = createLocationSub();
        location2.setCanonicalUri("unbox.amazon.co.uk/SOME_OTHER_ASIN/SUBSCRIPTION");
        Encoding encoding = createEncoding(location1);
        encoding.addAvailableAt(location2);
        Version version = createVersion(encoding);
        version.setCanonicalUri("unbox.amazon.co.uk/SOMEVERSIONID");
        Film film = createFilm(version);
        film.setId(1L);
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(film, version, encoding, ImmutableList.of(location1, location2));
        String onDemandImi = "onDemandImi";

        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, onDemandImi);

        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();

        Set<String> types = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_TYPE));
        assertEquals("other", Iterables.getOnlyElement(types));

        assertEquals("The resulting objecting either omitted genres, or included multiple copies of a genre.", 2, instanceDesc.getGenre().size());
        Set<String> hrefs = instanceDesc.getGenre().stream().map(ControlledTermType::getHref).collect(Collectors.toSet());
        assertTrue( hrefs.contains(AmazonOnDemandLocationGenerator.YOUVIEW_GENRE_MEDIA_AVAILABLE));
        assertTrue("YOUVIEW_ENTITLEMENT_SUBSCRIPTION was not included in the resulting object.", hrefs.contains(AmazonOnDemandLocationGenerator.YOUVIEW_ENTITLEMENT_SUBSCRIPTION));
    }
    @Test
    public void testNonPublisherSpecificFields() {
        Location location1 = createLocation1();
        location1.setCanonicalUri("unbox.amazon.co.uk/SOME_ASIN/PAY");
        Location location2 = createLocation2();
        location2.setCanonicalUri("unbox.amazon.co.uk/SOME_OTHER_ASIN/RENT");
        Encoding encoding = createEncoding(location1);
        encoding.addAvailableAt(location2);
        Version version = createVersion(encoding);
        version.setCanonicalUri("unbox.amazon.co.uk/SOMEVERSIONID");
        Film film = createFilm(version);
        film.setId(1L);
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(film, version, encoding, ImmutableList.of(location1, location2));
        String onDemandImi = "onDemandImi";
        
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, onDemandImi);

        assertEquals("P0DT1H30M0.000S", onDemand.getPublishedDuration().toString());
        assertEquals("2012-07-03T00:00:00Z", onDemand.getStartOfAvailability().toString());
        assertFalse(onDemand.getFree().isValue());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        
        Set<String> types = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_TYPE));
        assertEquals("other", Iterables.getOnlyElement(types));

        assertEquals(3, instanceDesc.getGenre().size());
        Set<String> hrefs = instanceDesc.getGenre().stream().map(ControlledTermType::getHref).collect(Collectors.toSet());
        assertTrue(hrefs.contains(AmazonOnDemandLocationGenerator.YOUVIEW_GENRE_MEDIA_AVAILABLE));
        assertTrue(hrefs.contains(AmazonOnDemandLocationGenerator.YOUVIEW_ENTITLEMENT_PAY_TO_BUY));
        assertTrue(hrefs.contains(AmazonOnDemandLocationGenerator.YOUVIEW_ENTITLEMENT_PAY_TO_RENT));

        AVAttributesType avAttributes = instanceDesc.getAVAttributes();
        AudioAttributesType audioAttrs = Iterables.getOnlyElement(avAttributes.getAudioAttributes());
        
        assertEquals("urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3", audioAttrs.getMixType().getHref());
        
        VideoAttributesType videoAttrs = avAttributes.getVideoAttributes();
        
        assertEquals(Integer.valueOf(1280), videoAttrs.getHorizontalSize());
        assertEquals(Integer.valueOf(720), videoAttrs.getVerticalSize());
        assertEquals("16:9", Iterables.getOnlyElement(videoAttrs.getAspectRatio()).getValue());

        assertEquals(BigInteger.valueOf(3308), avAttributes.getBitRate().getValue());
        assertFalse(avAttributes.getBitRate().isVariable());

        UniqueIDType deepLink = instanceDesc.getOtherIdentifier().get(0);
        UniqueIDType allContributingAsins = instanceDesc.getOtherIdentifier().get(1);

        assertEquals("asin.amazon.com", deepLink.getAuthority());
        assertEquals("SOME_ASIN", deepLink.getValue());
        assertEquals("ondemand.asin.amazon.com", allContributingAsins.getAuthority());
        assertEquals("SOME_OTHER_ASIN SOME_ASIN", allContributingAsins.getValue()); //If you made it to get all contributing IDS here, add a proper test for it.
    }
    
    @Test
    public void testUnboxSpecificFields() {
        Location location = createLocation1();
        location.setCanonicalUri("unbox.amazon.co.uk/SOMELOCATIONID/SUBSCRIPTION");
        Encoding encoding = createEncoding(location);
        Version version = createVersion(encoding);
        version.setCanonicalUri("unbox.amazon.co.uk/SOMELOCATIONID");
        Film film = createFilm(version);
        film.setId(10000L);
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(film, version, encoding, ImmutableList.of(location));
        String onDemandImi = "imi:amazon.com:stage-metabroadcast.com:content:szp:ondemand:HD";
        
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, onDemandImi);
        
        assertEquals("http://amazon.com/services/on_demand/primevideo", onDemand.getServiceIDRef());
        assertEquals("crid://stage-metabroadcast.com/amazon.com:content:szp:version", onDemand.getProgram().getCrid());
        assertEquals(onDemandImi, onDemand.getInstanceMetadataId());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        UniqueIDType deepLink = instanceDesc.getOtherIdentifier().get(0);
        UniqueIDType allContributingAsins = instanceDesc.getOtherIdentifier().get(1);
        assertEquals(AmazonOnDemandLocationGenerator.DEEP_LINKING_AUTHORITY, deepLink.getAuthority());
        assertEquals(AmazonOnDemandLocationGenerator.ALL_ASINS_AUTHORITY, allContributingAsins.getAuthority());
    }


    @Test public void testOndemandsAfterConsolidation() {
//        Film film = AmazonProgramInformationGeneratorTest.createConvolutedFilm();
//        Iterator<Version> vIter = film.getVersions().iterator();
//        Version version = vIter.next(); //there should be one after consolidation
//        ItemAndVersion versionHierarchy = new ItemAndVersion(film, version);
//        ProgramInformationType generate = generator.generate(
//                versionHierarchy,
//                version.getCanonicalUri()
//        );
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

        encoding.setQuality(Quality.HD);
        encoding.setVideoHorizontalSize(1280);
        encoding.setVideoVerticalSize(720);
        encoding.setVideoAspectRatio("16:9");
        encoding.setBitRate(3308);
        encoding.addAvailableAt(location);
        
        return encoding;
    }

    private Location createLocation1() {
        Location location = new Location();
        
        Policy policy = new Policy();
        policy.setRevenueContract(Policy.RevenueContract.PAY_TO_BUY);
        policy.setAvailabilityStart(new DateTime(2012, 7, 3, 0, 0, 0, DateTimeZone.UTC));
        policy.setAvailabilityEnd(new DateTime(2013, 7, 17, 0, 0, 0, DateTimeZone.UTC));
        
        location.setPolicy(policy);
        
        return location;
    }

    private Location createLocation2() {
        Location location = new Location();

        Policy policy = new Policy();
        policy.setRevenueContract(Policy.RevenueContract.PAY_TO_RENT);
        policy.setAvailabilityStart(new DateTime(2012, 7, 3, 0, 0, 0, DateTimeZone.UTC));
        policy.setAvailabilityEnd(new DateTime(2013, 7, 17, 0, 0, 0, DateTimeZone.UTC));

        location.setPolicy(policy);

        return location;
    }

    private Location createLocationSub() {
        Location location = new Location();

        Policy policy = new Policy();
        policy.setRevenueContract(Policy.RevenueContract.SUBSCRIPTION);
        policy.setAvailabilityStart(new DateTime(2012, 7, 3, 0, 0, 0, DateTimeZone.UTC));
        policy.setAvailabilityEnd(new DateTime(2013, 7, 17, 0, 0, 0, DateTimeZone.UTC));

        location.setPolicy(policy);

        return location;
    }
}
