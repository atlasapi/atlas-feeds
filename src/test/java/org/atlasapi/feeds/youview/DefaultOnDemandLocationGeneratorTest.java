package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Set;

import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.youview.genres.GenreMappings;
import org.atlasapi.feeds.youview.ids.IdParsers;
import org.atlasapi.feeds.youview.ids.PublisherIdUtilities;
import org.atlasapi.feeds.youview.images.ImageConfigurations;
import org.atlasapi.feeds.youview.transactions.TransactionStore;
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
import org.mockito.Mockito;

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
import com.google.common.collect.Sets;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.intl.Countries;
import com.youview.refdata.schemas._2011_07_06.ExtendedOnDemandProgramType;

public class DefaultOnDemandLocationGeneratorTest {
    
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
    
    private YouViewPerPublisherFactory configFactory = YouViewPerPublisherFactory.builder()
            .withPublisher(
                    Publisher.LOVEFILM, 
                    PublisherIdUtilities.idUtilFor(Publisher.LOVEFILM, "base uri"),
                    ImageConfigurations.imageConfigFor(Publisher.LOVEFILM),
                    IdParsers.parserFor(Publisher.LOVEFILM), 
                    GenreMappings.mappingFor(Publisher.LOVEFILM), 
                    Mockito.mock(SimpleHttpClient.class),
                    Mockito.mock(TransactionStore.class))
            .withPublisher(
                    Publisher.AMAZON_UNBOX, 
                    PublisherIdUtilities.idUtilFor(Publisher.AMAZON_UNBOX, "base uri"),
                    ImageConfigurations.imageConfigFor(Publisher.AMAZON_UNBOX),
                    IdParsers.parserFor(Publisher.AMAZON_UNBOX), 
                    GenreMappings.mappingFor(Publisher.AMAZON_UNBOX), 
                    Mockito.mock(SimpleHttpClient.class),
                    Mockito.mock(TransactionStore.class))
            .build();
    private final OnDemandLocationGenerator generator = new DefaultOnDemandLocationGenerator(configFactory);

    @Test
    public void testOnDemandNotCreatedWhenNoEncoding() {
        Film film = createLoveFilmFilm();
        
        Set<Version> versions = Sets.newHashSet(new Version());
        film.setVersions(versions);
        
        assertTrue("No element should be created when no encoding present", Iterables.isEmpty(generator.generate(film)));
    }
    
    @Test
    public void testNonPublisherSpecificFields() {
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) Iterables.getOnlyElement(generator.generate(createLoveFilmFilm()));

        assertEquals("P0DT1H30M0.000S", onDemand.getPublishedDuration().toString());
        assertEquals("2012-07-03T00:00:00.000Z", onDemand.getStartOfAvailability().toString());
        assertEquals("2013-07-17T00:00:00.000Z", onDemand.getEndOfAvailability().toString());
        assertFalse(onDemand.getFree().isValue());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        
        Set<String> hrefs = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_HREF));
        Set<String> types = ImmutableSet.copyOf(Iterables.transform(instanceDesc.getGenre(), GENRE_TO_TYPE));
                
        assertEquals("other", Iterables.getOnlyElement(types));
        
        Set<String> expected = ImmutableSet.of(
                "http://refdata.youview.com/mpeg7cs/YouViewMediaAvailabilityCS/2010-09-29#media_available",
                "http://refdata.youview.com/mpeg7cs/YouViewEntitlementTypeCS/2010-11-11#subscription");
        
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
    public void testLoveFilmSpecificFields() {
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) Iterables.getOnlyElement(generator.generate(createLoveFilmFilm()));
        
        assertEquals("http://lovefilm.com/OnDemand", onDemand.getServiceIDRef());
        assertEquals("crid://lovefilm.com/product/177221_version", onDemand.getProgram().getCrid());
        assertEquals("imi:lovefilm.com/177221", onDemand.getInstanceMetadataId());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        UniqueIDType otherId = Iterables.getOnlyElement(instanceDesc.getOtherIdentifier());
        assertEquals("deep_linking_id.lovefilm.com", otherId.getAuthority());
    }
    
    @Test
    public void testUnboxSpecificFields() {
        ExtendedOnDemandProgramType onDemand = (ExtendedOnDemandProgramType) Iterables.getOnlyElement(generator.generate(createUnboxFilm()));
        
        assertEquals("http://unbox.amazon.co.uk/OnDemand", onDemand.getServiceIDRef());
        assertEquals("crid://unbox.amazon.co.uk/product/177221_version", onDemand.getProgram().getCrid());
        assertEquals("imi:unbox.amazon.co.uk/177221", onDemand.getInstanceMetadataId());
        
        InstanceDescriptionType instanceDesc = onDemand.getInstanceDescription();
        UniqueIDType otherId = Iterables.getOnlyElement(instanceDesc.getOtherIdentifier());
        assertEquals("deep_linking_id.unbox.amazon.co.uk", otherId.getAuthority());
    }

    private Film createLoveFilmFilm() {
        Film film = new Film();
        
        film.setCanonicalUri("http://lovefilm.com/films/177221");
        film.setGenres(ImmutableList.of("http://lovefilm.com/genres/comedy"));
        film.setPublisher(Publisher.LOVEFILM);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.addAlias(new Alias("gb:amazon:asin", "filmAsin"));
        film.addVersion(createVersion());
        
        return film;
    }

    private Film createUnboxFilm() {
        Film film = new Film();
        
        film.setCanonicalUri("http://unbox.amazon.co.uk/movies/177221");
        film.setGenres(ImmutableList.of("http://unbox.amazon.co.uk/genres/comedy"));
        film.setPublisher(Publisher.AMAZON_UNBOX);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.addAlias(new Alias("gb:amazon:asin", "filmAsin"));
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
        version.addManifestedAs(encoding);
        version.setDuration(Duration.standardMinutes(90));
        return version;
    }
}
