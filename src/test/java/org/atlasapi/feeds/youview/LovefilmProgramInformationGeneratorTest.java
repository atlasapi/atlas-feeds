package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;
import org.junit.Test;

import tva.metadata._2010.ProgramInformationType;
import tva.metadata.extended._2010.ExtendedContentDescriptionType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;

public class LovefilmProgramInformationGeneratorTest {
    
    private static final ProgramInformationGenerator generator = new LoveFilmProgramInformationGenerator();

    @Test
    public void testFilmProgInfoGeneration() {
        ProgramInformationType progInfo = generator.generate(createFilm());

        // TODO this will change when we get the digital release id, is placeholder for now
        assertEquals("crid://lovefilm.com/product/177221_version", progInfo.getProgramId());
        UniqueIDType otherId = Iterables.getOnlyElement(progInfo.getOtherIdentifier());
        assertEquals("deep_linking_id.lovefilm.com", otherId.getAuthority());
        assertEquals("177221L", otherId.getValue());
        assertEquals("crid://lovefilm.com/product/177221", progInfo.getDerivedFrom().getCrid());
        
        ExtendedContentDescriptionType basicDescription = (ExtendedContentDescriptionType) progInfo.getBasicDescription();
        
        assertEquals("http://bbfc.org.uk/BBFCRatingCS/2002#PG", basicDescription.getParentalGuidance().getParentalRating().getHref());
        assertEquals("1963", basicDescription.getProductionDate().getTimePoint());
        // compare strings, as javax.xml.datatype.Duration is horrible to instantiate
        assertEquals("PT1H30M0.000S", basicDescription.getDuration().toString());
        assertEquals("gb", Iterables.getOnlyElement(basicDescription.getProductionLocation()));
    }

    @Test
    public void testEpisodeProgInfoGeneration() {
        ProgramInformationType progInfo = generator.generate(createEpisode());
        
        // TODO this will change when we get the digital release id, is placeholder for now
        assertEquals("crid://lovefilm.com/product/180014_version", progInfo.getProgramId());
        UniqueIDType otherId = Iterables.getOnlyElement(progInfo.getOtherIdentifier());
        assertEquals("deep_linking_id.lovefilm.com", otherId.getAuthority());
        assertEquals("180014L", otherId.getValue());
        assertEquals("crid://lovefilm.com/product/180014", progInfo.getDerivedFrom().getCrid());
        
        ExtendedContentDescriptionType basicDescription = (ExtendedContentDescriptionType) progInfo.getBasicDescription();
        
        assertEquals("http://bbfc.org.uk/BBFCRatingCS/2002#15", basicDescription.getParentalGuidance().getParentalRating().getHref());
        assertEquals("2006", basicDescription.getProductionDate().getTimePoint());
        // compare strings, as javax.xml.datatype.Duration is horrible to instantiate
        assertEquals("PT45M0.000S", basicDescription.getDuration().toString());
        assertEquals("gb", Iterables.getOnlyElement(basicDescription.getProductionLocation()));
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
        
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(45));
        episode.setVersions(ImmutableSet.of(version));
        
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
        
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(90));
        film.setVersions(ImmutableSet.of(version));
        return film;
    }

}
