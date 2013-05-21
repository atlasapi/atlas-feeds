package org.atlasapi.feeds.youview;

import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.math.BigInteger;
import java.util.Set;

import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
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
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.VideoAttributesType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.metabroadcast.common.intl.Countries;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;

public class LovefilmOnDemandLocationGeneratorTest {
    
    private static final OnDemandLocationGenerator generator = new LoveFilmOnDemandLocationGenerator();

    @Test
    public void testOnDemandNotCreatedWhenNoEncoding() {
        Film film = createFilm();
        
        Set<Version> versions = Sets.newHashSet(new Version());
        film.setVersions(versions);
        
        Optional<OnDemandProgramType> onDemand = generator.generate(film);
        assertFalse(onDemand.isPresent());
    }
    
    @Test
    public void testFilmOnDemandGeneration() {
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(createFilm()).get();

        assertEquals("http://lovefilm.com/OnDemand", onDemand.getServiceIDRef());
        // TODO this will change when we get the digital release id, is placeholder for now        
        assertEquals("crid://lovefilm.com/product/filmAsin_version", onDemand.getProgram().getCrid());
        // TODO this will change when we get the digital release id, is placeholder for now
        assertEquals("imi:lovefilm.com/filmAsin", onDemand.getInstanceMetadataId());
        assertEquals("PT1H30M0.000S", onDemand.getPublishedDuration().toString());
        assertEquals("2012-07-03T00:00:00.000Z", onDemand.getStartOfAvailability().toString());
        assertEquals("2013-07-17T00:00:00.000Z", onDemand.getEndOfAvailability().toString());
        assertFalse(onDemand.getFree().isValue());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        GenreType first = Iterables.get(instanceDesc.getGenre(), 0);
        GenreType second = Iterables.get(instanceDesc.getGenre(), 1);
        
        assertEquals(first.getType(), "other");
        assertEquals(second.getType(), "other");
        assertThat(first.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available",
            "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#subscription"
        ));
        assertThat(second.getHref(), isOneOf(
                "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available",
                "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#subscription"
            ));
        assertFalse(first.equals(second));

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
        assertEquals("deep_linking_id.lovefilm.com", otherId.getAuthority());
        assertEquals("filmAsin", otherId.getValue());
    }

    @Test
    public void testEpisodeOnDemandGeneration() {
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) generator.generate(createEpisode()).get();

        assertEquals("http://lovefilm.com/OnDemand", onDemand.getServiceIDRef());
        // TODO this will change when we get the digital release id, is placeholder for now        
        assertEquals("crid://lovefilm.com/product/episodeAsin_version", onDemand.getProgram().getCrid());
        // TODO this will change when we get the digital release id, is placeholder for now
        assertEquals("imi:lovefilm.com/episodeAsin", onDemand.getInstanceMetadataId());
        assertEquals("PT45M0.000S", onDemand.getPublishedDuration().toString());
        assertEquals("2009-09-21T00:00:00.000Z", onDemand.getStartOfAvailability().toString());
        assertEquals("2013-03-01T00:00:00.000Z", onDemand.getEndOfAvailability().toString());
        assertFalse(onDemand.getFree().isValue());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        
        GenreType first = Iterables.get(instanceDesc.getGenre(), 0);
        GenreType second = Iterables.get(instanceDesc.getGenre(), 1);
        
        assertEquals(first.getType(), "other");
        assertEquals(second.getType(), "other");
        assertThat(first.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available",
            "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#subscription"
        ));
        assertThat(second.getHref(), isOneOf(
                "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available",
                "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#subscription"
            ));
        assertFalse(first.equals(second));

        AVAttributesType avAttributes = instanceDesc.getAVAttributes();
        AudioAttributesType audioAttrs = Iterables.getOnlyElement(avAttributes.getAudioAttributes());
        
        assertEquals("urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3", audioAttrs.getMixType().getHref());
        
        VideoAttributesType videoAttrs = avAttributes.getVideoAttributes();
        
        assertEquals(Integer.valueOf(720), videoAttrs.getHorizontalSize());
        assertEquals(Integer.valueOf(576), videoAttrs.getVerticalSize());
        assertEquals("16:9", Iterables.getOnlyElement(videoAttrs.getAspectRatio()).getValue());

        assertEquals(BigInteger.valueOf(1600), avAttributes.getBitRate().getValue());
        assertFalse(avAttributes.getBitRate().isVariable());
        
        UniqueIDType otherId = Iterables.getOnlyElement(instanceDesc.getOtherIdentifier());
        assertEquals("deep_linking_id.lovefilm.com", otherId.getAuthority());
        assertEquals("episodeAsin", otherId.getValue());
    }
    
    private Episode createEpisode() {
        Episode episode = new Episode();
        
        episode.setCanonicalUri("http://lovefilm.com/episodes/180014");
        episode.setCurie("lf:e-180014");
        episode.setGenres(ImmutableList.of(
            "http://lovefilm.com/genres/comedy", 
            "http://lovefilm.com/genres/television"
        ));
        episode.setImage("http://www.lovefilm.com/lovefilm/images/products/heroshots/0/137640-large.jpg");
        episode.setPublisher(Publisher.LOVEFILM);
        episode.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        episode.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        episode.setYear(2006);
        episode.addAlias(new Alias("gb:amazon:asin", "episodeAsin"));
        
        Version version = new Version();
        Encoding encoding = new Encoding();
        encoding.setVideoHorizontalSize(720);
        encoding.setVideoVerticalSize(576);
        encoding.setVideoAspectRatio("16:9");
        encoding.setBitRate(1600);
        
        
        Location location = new Location();
        Policy policy = new Policy();

        policy.setAvailabilityStart(new DateTime(2009, 9, 21, 0, 0, 0, DateTimeZone.UTC));
        policy.setAvailabilityEnd(new DateTime(2013, 3, 1, 0, 0, 0, DateTimeZone.UTC));
        
        location.setPolicy(policy);
        encoding.addAvailableAt(location);
        version.addManifestedAs(encoding);
        version.setDuration(Duration.standardMinutes(45));
        episode.addVersion(version);
        
        return episode;
    }

    private Film createFilm() {
        Film film = new Film();
        
        film.setCanonicalUri("http://lovefilm.com/films/177221");
        film.setCurie("lf:f-177221");
        film.setGenres(ImmutableList.of("http://lovefilm.com/genres/comedy"));
        film.setImage("http://www.lovefilm.com/lovefilm/images/products/heroshots/1/177221-large.jpg");
        film.setPublisher(Publisher.LOVEFILM);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.addAlias(new Alias("gb:amazon:asin", "filmAsin"));
        
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
        version.addManifestedAs(encoding);
        version.setDuration(Duration.standardMinutes(90));
        film.addVersion(version);
        
        return film;
    }

}
