package org.atlasapi.feeds.youview.unbox;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Certificate;
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

public class UnboxProgramInformationGeneratorTest {
    
    private IdGenerator idGenerator = new UnboxIdGenerator();
    
    private final ProgramInformationGenerator generator = new UnboxProgramInformationGenerator(idGenerator);

    @Test
    public void testPublisherIndependentFields() {
        Version version = createVersion();
        Film film = createFilm(version);
        ItemAndVersion versionHierarchy = new ItemAndVersion(film, version);
        String versionCrid = "versionCrid";
        
        ProgramInformationType progInfo = generator.generate(versionHierarchy, versionCrid);

        UniqueIDType otherId = Iterables.getOnlyElement(progInfo.getOtherIdentifier());
        assertEquals("filmAsin", otherId.getValue());
        
        ExtendedContentDescriptionType basicDescription = (ExtendedContentDescriptionType) progInfo.getBasicDescription();
        
        assertEquals("http://bbfc.org.uk/BBFCRatingCS/2002#PG", basicDescription.getParentalGuidance().getParentalRating().getHref());
        assertEquals("1963", basicDescription.getProductionDate().getTimePoint());
        // compare strings, as javax.xml.datatype.Duration is horrible to instantiate
        assertEquals("P0DT1H30M0.000S", basicDescription.getDuration().toString());
        assertEquals("gb", Iterables.getOnlyElement(basicDescription.getProductionLocation()));
    }
    
    @Test
    public void testUnboxSpecificFields() {
        Version version = createVersion();
        Film film = createFilm(version);
        ItemAndVersion versionHierarchy = new ItemAndVersion(film, version);
        String versionCrid = "versionCrid";
        
        ProgramInformationType progInfo = generator.generate(versionHierarchy, versionCrid);
        
        assertEquals(versionCrid, progInfo.getProgramId());
        UniqueIDType otherId = Iterables.getOnlyElement(progInfo.getOtherIdentifier());
        assertEquals("deep_linking_id.unbox.amazon.co.uk", otherId.getAuthority());
        assertEquals("crid://amazon.com/product/177221", progInfo.getDerivedFrom().getCrid());
    }

    private Film createFilm(Version version) {
        Film film = new Film();
        
        film.setCanonicalUri("http://unbox.amazon.co.uk/movies/177221");
        film.setPublisher(Publisher.AMAZON_UNBOX);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.addAlias(new Alias("gb:amazon:asin", "filmAsin"));
        
        film.setVersions(ImmutableSet.of(version));
        return film;
    }

    private Version createVersion() {
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(90));
        return version;
    }
}
