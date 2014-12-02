package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.atlasapi.feeds.tvanytime.granular.GranularProgramInformationGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Restriction;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;
import org.junit.Test;
import org.mockito.Mockito;

import tva.metadata._2010.BasicContentDescriptionType;
import tva.metadata._2010.ProgramInformationType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.hash.HashFunction;
import com.metabroadcast.common.intl.Countries;


public class NitroProgramInformationGeneratorTest {
    
    private IdGenerator idGenerator = new NitroIdGenerator(Mockito.mock(HashFunction.class));
    
    private final GranularProgramInformationGenerator generator = new NitroProgramInformationGenerator(idGenerator);

    @Test
    public void testPublisherIndependentFields() {
        String restrictionMessage = "This is a warning";
        Version version = createRestrictedVersion(Duration.standardMinutes(90), restrictionMessage);
        Film film = createNitroFilm(ImmutableSet.of(version));
        ItemAndVersion hierarchy = new ItemAndVersion(film, version);
        
        ProgramInformationType progInfo = generator.generate(hierarchy, "version_crid");

        BasicContentDescriptionType basicDescription = progInfo.getBasicDescription();
        
        assertEquals("urn:dtg:metadata:cs:DTGContentWarningCS:2011:W", basicDescription.getParentalGuidance().getParentalRating().getHref());
        assertEquals("1963", basicDescription.getProductionDate().getTimePoint());
        // compare strings, as javax.xml.datatype.Duration is horrible to instantiate
        assertEquals("P0DT1H30M0.000S", basicDescription.getDuration().toString());
        assertEquals("gb", Iterables.getOnlyElement(basicDescription.getProductionLocation()));
    }

    @Test
    public void testNitroSpecificFields() {
        Version version = createBaseVersion(Duration.standardMinutes(90));
        Film film = createNitroFilm(ImmutableSet.of(version));
        
        String versionCrid = idGenerator.generateVersionCrid(film, version);
        
        ProgramInformationType progInfo = generator.generate(new ItemAndVersion(film, version), versionCrid);
        
        assertEquals(versionCrid, progInfo.getProgramId());
        assertEquals(idGenerator.generateContentCrid(film), progInfo.getDerivedFrom().getCrid());
    }
    
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
    
    private Version createBaseVersion(Duration duration) {
        Version version = new Version();
        version.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b01sjkjf");
        version.setDuration(duration);

        return version;
    }

    private Version createRestrictedVersion(Duration duration, String restrictionMessage) {
        Version version = createBaseVersion(duration);

        Restriction restriction = new Restriction();
        restriction.setRestricted(true);
        restriction.setMessage(restrictionMessage);
        version.setRestriction(restriction);

        return version;
    }
}
