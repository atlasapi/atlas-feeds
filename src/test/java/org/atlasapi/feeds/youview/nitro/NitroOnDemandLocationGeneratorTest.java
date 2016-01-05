package org.atlasapi.feeds.youview.nitro;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
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
import tva.metadata._2010.AudioLanguageType;
import tva.metadata._2010.CaptionLanguageType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.SignLanguageType;
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

    private static final String ON_DEMAND_IMI = "on_demand_imi";
    private static final int VIDEO_BITRATE = 1500000;

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
    
    private static final DateTime DEFAULT_AVAILABILITY_END = new DateTime(2013, 7, 17, 0, 0, 0, DateTimeZone.UTC);

    private IdGenerator idGenerator = new NitroIdGenerator(Hashing.md5());

    private final OnDemandLocationGenerator generator = new NitroOnDemandLocationGenerator(idGenerator);

    @Test
    public void testNonPublisherSpecificFields() {
        ItemOnDemandHierarchy onDemandHierarchy = hierarchyFrom(createNitroFilm(true, DEFAULT_AVAILABILITY_END));
        
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, ON_DEMAND_IMI);

        assertEquals("P0DT1H30M0.000S", onDemand.getPublishedDuration().toString());
        assertEquals("2012-07-03T00:00:00Z", onDemand.getStartOfAvailability().toString());
        assertEquals("2013-07-17T00:00:00Z", onDemand.getEndOfAvailability().toString());
        assertTrue(onDemand.getFree().isValue());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        
        CaptionLanguageType captionLanguage = Iterables.getOnlyElement(instanceDesc.getCaptionLanguage());

        assertTrue(captionLanguage.isClosed());
        assertEquals(captionLanguage.getValue(), "en");

        List<SignLanguageType> signLanguages = instanceDesc.getSignLanguage();
        SignLanguageType signLanguageType = Iterables.getOnlyElement(signLanguages);
        assertEquals("bfi", signLanguageType.getValue());

        AVAttributesType avAttributes = instanceDesc.getAVAttributes();
        AudioAttributesType audioAttrs = Iterables.getOnlyElement(avAttributes.getAudioAttributes());
        
        assertEquals("urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3", audioAttrs.getMixType().getHref());
        
        VideoAttributesType videoAttrs = avAttributes.getVideoAttributes();
        
        assertEquals(Integer.valueOf(1280), videoAttrs.getHorizontalSize());
        assertEquals(Integer.valueOf(720), videoAttrs.getVerticalSize());
        assertEquals("16:9", Iterables.getOnlyElement(videoAttrs.getAspectRatio()).getValue());

        assertEquals(BigInteger.valueOf(VIDEO_BITRATE), avAttributes.getBitRate().getValue());
        assertTrue(avAttributes.getBitRate().isVariable());

        UniqueIDType otherId = Iterables.getOnlyElement(instanceDesc.getOtherIdentifier());
        assertEquals("b020tm1g", otherId.getValue());
        assertEquals("epid.bbc.co.uk", otherId.getAuthority());
    }
    
    @Test
    public void testNitroSpecificFields() {
        Film film = createNitroFilm(false, DEFAULT_AVAILABILITY_END);
        ItemOnDemandHierarchy hierarchy = hierarchyFrom(film);
        String versionCrid = idGenerator.generateVersionCrid(hierarchy.item(), hierarchy.version());
        String onDemandImi = idGenerator.generateOnDemandImi(hierarchy.item(), hierarchy.version(), hierarchy.encoding(), hierarchy.location());
        
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(hierarchy, onDemandImi);
        
        
        assertEquals("http://nitro.bbc.co.uk/services/youview", onDemand.getServiceIDRef());
        assertEquals(versionCrid, onDemand.getProgram().getCrid());
        assertEquals(onDemandImi, onDemand.getInstanceMetadataId());

        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();

        AVAttributesType avAttributes = instanceDesc.getAVAttributes();
        List<AudioAttributesType> audioAttributes = avAttributes.getAudioAttributes();
        AudioAttributesType audioAttribute = Iterables.getOnlyElement(audioAttributes);
        AudioLanguageType audioLanguage = audioAttribute.getAudioLanguage();

        assertEquals("urn:tva:metadata:cs:AudioPurposeCS:2007:1", audioLanguage.getPurpose());
        assertEquals(true, audioLanguage.isSupplemental());
        assertEquals("dubbed", audioLanguage.getType());
    }

    @Test
    public void testGaelicLanguageIsSetForSubtitlesOnAlbaChannel() {
        Film film = createAlbaNitroFilm();
        ExtendedOnDemandProgramType onDemand = onDemandFor(film);

        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        CaptionLanguageType captionLanguage = Iterables.getOnlyElement(instanceDesc.getCaptionLanguage());

        assertTrue(captionLanguage.isClosed());
        assertEquals(captionLanguage.getValue(), "gla");
    }
    
    @Test
    public void testSubtitledFlag() {
        CaptionLanguageType captionLanguage = 
                Iterables.getOnlyElement(
                        onDemandFor(createNitroFilm(true, DEFAULT_AVAILABILITY_END))
                            .getInstanceDescription()
                            .getCaptionLanguage()
                        );
        
        assertTrue(captionLanguage.isClosed());
        assertEquals("en", captionLanguage.getValue());
        
        assertTrue(onDemandFor(createNitroFilm(false, DEFAULT_AVAILABILITY_END))
                         .getInstanceDescription()
                         .getCaptionLanguage()
                         .isEmpty());
    }
    
    private ExtendedOnDemandProgramType onDemandFor(Item item) {
        ItemOnDemandHierarchy hierarchy = hierarchyFrom(item);
        String onDemandImi = idGenerator.generateOnDemandImi(hierarchy.item(), hierarchy.version(), hierarchy.encoding(), hierarchy.location());
        return (ExtendedOnDemandProgramType) generator.generate(hierarchy, onDemandImi);
    }
    
    @Test
    public void testIfNoActualAvailabilityThenContentNotMarkedAsAvailable() {
        ItemOnDemandHierarchy onDemandHierarchy = hierarchyFrom(createNitroFilm(false, DEFAULT_AVAILABILITY_END));
        
        onDemandHierarchy.location().getPolicy().setActualAvailabilityStart(null);
        
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, ON_DEMAND_IMI);
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        Set<String> hrefs = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_HREF));
        
        assertTrue("No 'media available' genre should be added if no actual availability has been identified", hrefs.isEmpty());
        
        assertEquals("2012-07-03T00:00:00Z", onDemand.getStartOfAvailability().toString());
        assertEquals("2013-07-17T00:00:00Z", onDemand.getEndOfAvailability().toString());
    }

    @Test
    public void testIfActualAvailabilityPresentThenContentMarkedAsAvailable() {
        ItemOnDemandHierarchy onDemandHierarchy = hierarchyFrom(createNitroFilm(false, DEFAULT_AVAILABILITY_END));

        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, ON_DEMAND_IMI);

        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        Set<String> hrefs = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_HREF));
        Set<String> types = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_TYPE));

        assertEquals("http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available", getOnlyElement(hrefs));
        assertEquals("other", getOnlyElement(types));

        assertEquals("2012-07-03T00:00:00Z", onDemand.getStartOfAvailability().toString());
        assertEquals("2013-07-17T00:00:00Z", onDemand.getEndOfAvailability().toString());
    }
    
    @Test
    public void testIfNoEndOfAvailabilityNullAvailabilityEnd() {
        ItemOnDemandHierarchy onDemandHierarchy = hierarchyFrom(createNitroFilm(false, null));
        
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, ON_DEMAND_IMI);
        assertNull(onDemand.getEndOfAvailability());
    }
    
    private ItemOnDemandHierarchy hierarchyFrom(Item item) {
        Version version = Iterables.getOnlyElement(item.getVersions());
        Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());
        Location location = Iterables.getOnlyElement(encoding.getAvailableAt());
        return new ItemOnDemandHierarchy(item, version, encoding, location);
    }

    private Film createNitroFilm(boolean subtitled, DateTime availabilityEnd) {
        Film film = new Film();

        film.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b020tm1g");
        film.setPublisher(Publisher.BBC_NITRO);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setYear(1963);
        film.addVersion(createVersion(subtitled, availabilityEnd));

        return film;
    }

    private Film createAlbaNitroFilm() {
        Film film = createNitroFilm(true, DEFAULT_AVAILABILITY_END);
        film.setPresentationChannel("http://ref.atlasapi.org/channels/bbcalba");
        
        return film;
    }

    private Version createVersion(boolean subtitled, DateTime availabilityEnd) {
        Version version = new Version();

        Restriction restriction = new Restriction();
        restriction.setRestricted(true);
        
        version.setManifestedAs(Sets.newHashSet(createEncoding(subtitled, availabilityEnd)));
        
        version.setDuration(Duration.standardMinutes(90));
        version.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b00gszl0");
        version.setRestriction(restriction);
        
        return version;
    }

    private Encoding createEncoding(boolean subtitled, DateTime availabilityEnd) {
        Encoding encoding = new Encoding();
        encoding.setVideoHorizontalSize(1280);
        encoding.setVideoVerticalSize(720);
        encoding.setVideoAspectRatio("16:9");
        encoding.setVideoBitRate(VIDEO_BITRATE);
        encoding.setAudioDescribed(true);
        encoding.setSigned(true);
        encoding.setSubtitled(subtitled);
        encoding.addAvailableAt(createLocation(availabilityEnd));
        
        return encoding;
    }

    private Location createLocation(DateTime availabilityEnd) {
        Location location = new Location();

        Policy policy = new Policy();

        policy.setActualAvailabilityStart(new DateTime(2012, 7, 3, 0, 10, 0, DateTimeZone.UTC));
        policy.setAvailabilityStart(new DateTime(2012, 7, 3, 0, 0, 0, DateTimeZone.UTC));
        policy.setAvailabilityEnd(availabilityEnd);

        location.setPolicy(policy);

        return location;
    }
}
