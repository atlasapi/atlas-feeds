package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Set;

import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Restriction;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.metabroadcast.common.intl.Countries;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;

public class NitroOnDemandLocationGeneratorTest {
    
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

    private IdGenerator idGenerator = new NitroIdGenerator(Hashing.md5());
    private OnDemandHierarchyExpander hierarchyExpander = new OnDemandHierarchyExpander(idGenerator);

    private final OnDemandLocationGenerator generator = new NitroOnDemandLocationGenerator(idGenerator, hierarchyExpander);

    @Test
    public void testOnDemandNotCreatedWhenNoEncoding() {
        Film film = createNitroFilm();
        
        Set<Version> versions = Sets.newHashSet(new Version());
        film.setVersions(versions);
        
        assertTrue("No element should be created when no encoding present", Iterables.isEmpty(generator.generate(film)));
    }
    
    @Test
    public void testNonPublisherSpecificFields() {
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) Iterables.getOnlyElement(generator.generate(createNitroFilm()));

        assertEquals("P0DT1H30M0.000S", onDemand.getPublishedDuration().toString());
        assertEquals("2012-07-03T01:00:00Z", onDemand.getStartOfAvailability().toString());
        assertEquals("2013-07-17T01:00:00Z", onDemand.getEndOfAvailability().toString());
        assertTrue(onDemand.getFree().isValue());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        
        Set<String> hrefs = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_HREF));
        Set<String> types = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_TYPE));
                
        assertEquals("other", Iterables.getOnlyElement(types));
        
        Set<String> expected = ImmutableSet.of("http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available");
        
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
        assertEquals("b00gszl0.imi:bbc.co.uk/pips/65751802", otherId.getValue());
    }
    
    @Test
    public void testNitroSpecificFields() {
        Film film = createNitroFilm();
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) Iterables.getOnlyElement(generator.generate(film));
        
        ItemOnDemandHierarchy hierarchy = hierarchyFrom(film);
        String versionCrid = idGenerator.generateVersionCrid(hierarchy.item(), hierarchy.version());
        String onDemandImi = idGenerator.generateOnDemandImi(hierarchy.item(), hierarchy.version(), hierarchy.encoding(), hierarchy.location());
        
        assertEquals("http://bbc.couk/services/youview", onDemand.getServiceIDRef());
        assertEquals(versionCrid, onDemand.getProgram().getCrid());
        assertEquals(onDemandImi, onDemand.getInstanceMetadataId());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        UniqueIDType otherId = Iterables.getOnlyElement(instanceDesc.getOtherIdentifier());
        assertEquals("www.bbc.co.uk", otherId.getAuthority());
    }

    private ItemOnDemandHierarchy hierarchyFrom(Film film) {
        Version version = Iterables.getOnlyElement(film.getVersions());
        Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());
        Location location = Iterables.getOnlyElement(encoding.getAvailableAt());
        return new ItemOnDemandHierarchy(film, version, encoding, location);
    }

    private Film createNitroFilm() {
        Film film = new Film();
        
        film.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b020tm1g");
        film.setPublisher(Publisher.BBC_NITRO);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setYear(1963);
        film.addVersion(createVersion());
        
        return film;
    }

    private Version createVersion() {
        Version version = new Version();
        
        Encoding encoding = new Encoding();
        encoding.setVideoHorizontalSize(1280);
        encoding.setVideoVerticalSize(720);
        encoding.setVideoAspectRatio("16:9");
        encoding.setBitRate(3308);
        
        Location location = new Location();
        
        Policy policy = new Policy();

        policy.setAvailabilityStart(new DateTime(2012, 7, 3, 0, 0, 0, DateTimeZone.UTC));
        policy.setAvailabilityEnd(new DateTime(2013, 7, 17, 0, 0, 0, DateTimeZone.UTC));
        
        location.setPolicy(policy);
        
        encoding.addAvailableAt(location);
        
        Restriction restriction = new Restriction();
        restriction.setRestricted(true);
        
        version.addManifestedAs(encoding);
        version.setDuration(Duration.standardMinutes(90));
        version.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b00gszl0");
        version.setRestriction(restriction);
        
        return version;
    }
}