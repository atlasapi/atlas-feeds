package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroProgramInformationGenerator;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Restriction;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;
import org.junit.Test;
import org.mockito.Mockito;

import tva.metadata._2010.ProgramInformationType;
import tva.metadata.extended._2010.ExtendedContentDescriptionType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.hash.HashFunction;
import com.metabroadcast.common.intl.Countries;


public class NitroProgramInformationGeneratorTest {
    
    private IdGenerator idGenerator = new NitroIdGenerator(Mockito.mock(BbcServiceIdResolver.class), Mockito.mock(HashFunction.class));
    private TvAnytimeElementFactory elementFactory = new TvAnytimeElementFactory();
    private final ProgramInformationGenerator generator = new NitroProgramInformationGenerator(idGenerator, elementFactory);

    @Test
    public void testPublisherIndependentFields() {
        Version version = createVersion(Duration.standardMinutes(90), true);
        Film film = createNitroFilm(ImmutableSet.of(version));
        
        ProgramInformationType progInfo = Iterables.getOnlyElement(generator.generate(film));

        ExtendedContentDescriptionType basicDescription = (ExtendedContentDescriptionType) progInfo.getBasicDescription();
        
        assertEquals("http://bbfc.org.uk/BBFCRatingCS/2002#PG", basicDescription.getParentalGuidance().getParentalRating().getHref());
        assertEquals("1963", basicDescription.getProductionDate().getTimePoint());
        // compare strings, as javax.xml.datatype.Duration is horrible to instantiate
        assertEquals("P0DT1H30M0.000S", basicDescription.getDuration().toString());
        assertEquals("gb", Iterables.getOnlyElement(basicDescription.getProductionLocation()));
    }

    @Test
    public void testNitroSpecificFields() {
        Version version = createVersion(Duration.standardMinutes(90), false);
        
        Film film = createNitroFilm(ImmutableSet.of(version));
        ProgramInformationType progInfo = Iterables.getOnlyElement(generator.generate(film));
        
        assertEquals(idGenerator.generateVersionCrid(film, version), progInfo.getProgramId());
        assertEquals(idGenerator.generateContentCrid(film), progInfo.getDerivedFrom().getCrid());
    }
    
    // TODO test multiple versions created if multiple ids are created for a set of versions on an item
    

    private Film createNitroFilm(Set<Version> versions) {
        Film film = new Film();
        
        film.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b01qyvnk");
        film.setCurie("lf:f-177221");
        film.setPublisher(Publisher.BBC_NITRO);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.setVersions(versions);
        
        return film;
    }
    
    private Version createVersion(Duration duration, boolean isRestricted) {
        Version version = new Version();
        
        version.setDuration(duration);
        Restriction restriction = new Restriction();
        restriction.setRestricted(isRestricted);
        version.setRestriction(restriction);
        return version;
    }
}
