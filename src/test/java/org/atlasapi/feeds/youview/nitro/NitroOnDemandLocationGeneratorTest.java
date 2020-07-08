package org.atlasapi.feeds.youview.nitro;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

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

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;

import tva.metadata._2010.AVAttributesType;
import tva.metadata._2010.AudioAttributesType;
import tva.metadata._2010.AudioLanguageType;
import tva.metadata._2010.CaptionLanguageType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.SignLanguageType;
import tva.metadata._2010.VideoAttributesType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.metabroadcast.common.intl.Countries;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;

public class NitroOnDemandLocationGeneratorTest {

    private static final String ON_DEMAND_IMI = "on_demand_imi";
    private static final int VIDEO_BITRATE = 1500000;

    private static final Duration DEFAULT_AVAILABILITY_END = Duration.standardDays(14);

    private IdGenerator idGenerator;
    private OnDemandLocationGenerator generator;
    private DateTime now;

    @Before
    public void setUp() throws Exception {
        idGenerator = new NitroIdGenerator(Hashing.md5());
        generator = new NitroOnDemandLocationGenerator(idGenerator);
        now = DateTime.now(DateTimeZone.UTC);
    }

    @Test
    public void testNonPublisherSpecificFields() {
        ItemOnDemandHierarchy onDemandHierarchy = hierarchyFrom(createNitroFilm(
                true,
                DEFAULT_AVAILABILITY_END
        ));

        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(
                onDemandHierarchy,
                ON_DEMAND_IMI
        );

        assertEquals("P0DT1H30M0.000S", onDemand.getPublishedDuration().toString());

        DateTime expectedStart = now.minusMinutes(60);
        DateTime expectedEnd = now.plus(DEFAULT_AVAILABILITY_END);

        assertEquals(
                expectedStart.toString("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                onDemand.getStartOfAvailability().toString()
        );
        assertEquals(
                expectedEnd.toString("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                onDemand.getEndOfAvailability().toString()
        );

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

        assertEquals(
                "urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3",
                audioAttrs.getMixType().getHref()
        );
        
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
        String onDemandImi = idGenerator.generateOnDemandImi(hierarchy.item(), hierarchy.version(), hierarchy.encoding(), hierarchy.locations());
        
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
        String onDemandImi = idGenerator.generateOnDemandImi(hierarchy.item(), hierarchy.version(), hierarchy.encoding(), hierarchy.locations());
        return (ExtendedOnDemandProgramType) generator.generate(hierarchy, onDemandImi);
    }
    
    @Test
    public void testIfNoActualAvailabilityThenContentNotMarkedAsAvailable() {
        ItemOnDemandHierarchy onDemandHierarchy = hierarchyFrom(createNitroFilm(false, DEFAULT_AVAILABILITY_END));
        
        onDemandHierarchy.locations().get(0).getPolicy().setActualAvailabilityStart(null);
        
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, ON_DEMAND_IMI);
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        Set<String> hrefs = ImmutableSet.copyOf(instanceDesc.getGenre()
                .stream()
                .map(ControlledTermType::getHref)
                .collect(Collectors.toList()));
        
        assertTrue("No 'media available' genre should be added if no actual availability has been identified", hrefs.isEmpty());
    }

    @Test
    public void testIfActualAvailabilityPresentThenContentMarkedAsAvailable() {
        ItemOnDemandHierarchy onDemandHierarchy = hierarchyFrom(createNitroFilm(false, DEFAULT_AVAILABILITY_END));

        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(onDemandHierarchy, ON_DEMAND_IMI);

        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        Set<String> hrefs = ImmutableSet.copyOf(instanceDesc.getGenre()
                .stream()
                .map(ControlledTermType::getHref)
                .collect(Collectors.toList()));
        Set<String> types = ImmutableSet.copyOf(instanceDesc.getGenre()
                .stream()
                .map(GenreType::getType)
                .collect(Collectors.toList()));

        assertEquals(
                "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available",
                getOnlyElement(hrefs)
        );
        assertEquals("other", getOnlyElement(types));
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
        return new ItemOnDemandHierarchy(item, version, encoding,  ImmutableList.of(location));
    }

    private Film createNitroFilm(boolean subtitled, Duration availability) {
        Film film = new Film();

        film.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b020tm1g");
        film.setPublisher(Publisher.BBC_NITRO);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setYear(1963);
        film.addVersion(createVersion(subtitled, availability));

        return film;
    }

    private Film createAlbaNitroFilm() {
        Film film = createNitroFilm(true, DEFAULT_AVAILABILITY_END);
        film.setPresentationChannel("http://ref.atlasapi.org/channels/bbcalba");
        
        return film;
    }

    private Version createVersion(boolean subtitled, Duration availability) {
        Version version = new Version();

        Restriction restriction = new Restriction();
        restriction.setRestricted(true);
        
        version.setManifestedAs(Sets.newHashSet(createEncoding(subtitled, availability)));
        
        version.setDuration(Duration.standardMinutes(90));
        version.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b00gszl0");
        version.setRestriction(restriction);
        
        return version;
    }

    private Encoding createEncoding(boolean subtitled, Duration availability) {
        Encoding encoding = new Encoding();
        encoding.setVideoHorizontalSize(1280);
        encoding.setVideoVerticalSize(720);
        encoding.setVideoAspectRatio("16:9");
        encoding.setVideoBitRate(VIDEO_BITRATE);
        encoding.setAudioDescribed(true);
        encoding.setSigned(true);
        encoding.setSubtitled(subtitled);
        encoding.addAvailableAt(createLocation(availability));
        
        return encoding;
    }

    private Location createLocation(@Nullable Duration availability) {
        Location location = new Location();

        Policy policy = new Policy();

        policy.setActualAvailabilityStart(now.minusMinutes(50));
        policy.setAvailabilityStart(now.minusMinutes(60));

        if (availability != null) {
            policy.setAvailabilityEnd(now.plus(availability));
        } else {
            policy.setAvailabilityEnd(null);
        }

        location.setPolicy(policy);

        return location;
    }
}
