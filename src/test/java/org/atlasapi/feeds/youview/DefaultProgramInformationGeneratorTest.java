package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;
import org.junit.Test;
import org.mockito.Mockito;

import tva.metadata._2010.ProgramInformationType;
import tva.metadata.extended._2010.ExtendedContentDescriptionType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.intl.Countries;

public class DefaultProgramInformationGeneratorTest {
    
    private YouViewPerPublisherFactory configFactory = YouViewPerPublisherFactory.builder()
            .withPublisher(Publisher.LOVEFILM, new LoveFilmPublisherConfiguration("base uri"), new LoveFilmIdParser(), new LoveFilmGenreMapping(), Mockito.mock(SimpleHttpClient.class))
            .withPublisher(Publisher.AMAZON_UNBOX, new UnboxPublisherConfiguration("base uri"), new UnboxIdParser(), new UnboxGenreMapping(), Mockito.mock(SimpleHttpClient.class))
            .build();
    private final ProgramInformationGenerator generator = new DefaultProgramInformationGenerator(configFactory);

    @Test
    public void testPublisherIndependentFields() {
        ProgramInformationType progInfo = generator.generate(createLoveFilmFilm());

        UniqueIDType otherId = Iterables.getOnlyElement(progInfo.getOtherIdentifier());
        assertEquals("filmAsin", otherId.getValue());
        
        ExtendedContentDescriptionType basicDescription = (ExtendedContentDescriptionType) progInfo.getBasicDescription();
        
        assertEquals("http://bbfc.org.uk/BBFCRatingCS/2002#PG", basicDescription.getParentalGuidance().getParentalRating().getHref());
        assertEquals("1963", basicDescription.getProductionDate().getTimePoint());
        // compare strings, as javax.xml.datatype.Duration is horrible to instantiate
        assertEquals("PT1H30M0.000S", basicDescription.getDuration().toString());
        assertEquals("gb", Iterables.getOnlyElement(basicDescription.getProductionLocation()));
    }
    
    @Test
    public void testLoveFilmSpecificFields() {
        ProgramInformationType progInfo = generator.generate(createLoveFilmFilm());
        
        assertEquals("crid://lovefilm.com/product/177221_version", progInfo.getProgramId());
        UniqueIDType otherId = Iterables.getOnlyElement(progInfo.getOtherIdentifier());
        assertEquals("deep_linking_id.lovefilm.com", otherId.getAuthority());
        assertEquals("crid://lovefilm.com/product/177221", progInfo.getDerivedFrom().getCrid());
    }
    
    @Test
    public void testUnboxSpecificFields() {
        ProgramInformationType progInfo = generator.generate(createUnboxFilm());
        
        assertEquals("crid://unbox.amazon.co.uk/product/177221_version", progInfo.getProgramId());
        UniqueIDType otherId = Iterables.getOnlyElement(progInfo.getOtherIdentifier());
        assertEquals("deep_linking_id.unbox.amazon.co.uk", otherId.getAuthority());
        assertEquals("crid://unbox.amazon.co.uk/product/177221", progInfo.getDerivedFrom().getCrid());
    }

    private Film createLoveFilmFilm() {
        Film film = new Film();
        
        film.setCanonicalUri("http://lovefilm.com/films/177221");
        film.setCurie("lf:f-177221");
        film.setPublisher(Publisher.LOVEFILM);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.addAlias(new Alias("gb:amazon:asin", "filmAsin"));
        
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(90));
        film.setVersions(ImmutableSet.of(version));
        return film;
    }

    private Film createUnboxFilm() {
        Film film = new Film();
        
        film.setCanonicalUri("http://unbox.amazon.co.uk/movies/177221");
        film.setPublisher(Publisher.AMAZON_UNBOX);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.addAlias(new Alias("gb:amazon:asin", "filmAsin"));
        
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(90));
        film.setVersions(ImmutableSet.of(version));
        return film;
    }

}
