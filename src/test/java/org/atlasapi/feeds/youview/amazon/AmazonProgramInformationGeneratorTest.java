package org.atlasapi.feeds.youview.amazon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.youview.YouViewGeneratorUtils;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Quality;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;
import org.junit.Test;
import tva.metadata._2010.ProgramInformationType;
import tva.metadata.extended._2010.ExtendedContentDescriptionType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AmazonProgramInformationGeneratorTest extends org.atlasapi.TestsWithConfiguration {

    private IdGenerator idGenerator = new AmazonIdGenerator();
    private final ProgramInformationGenerator generator = new AmazonProgramInformationGenerator(idGenerator);

    @Test
    public void testPublisherIndependentFields() {
        Version version = createVersion();
        Film film = createFilm(version);
        ItemAndVersion versionHierarchy = new ItemAndVersion(film, version);
        String versionCrid = "versionCrid";
        
        ProgramInformationType progInfo = generator.generate(versionHierarchy, versionCrid);

        ExtendedContentDescriptionType basicDescription = (ExtendedContentDescriptionType) progInfo.getBasicDescription();
        
        assertEquals("http://bbfc.org.uk/BBFCRatingCS/2002#15", basicDescription.getParentalGuidance().getParentalRating().getHref());
        assertEquals("1963", basicDescription.getProductionDate().getTimePoint());
        // compare strings, as javax.xml.datatype.Duration is horrible to instantiate
        assertEquals("P0DT1H30M0.000S", basicDescription.getDuration().toString());
        assertEquals("gb", Iterables.getOnlyElement(basicDescription.getProductionLocation()));
    }
    
    @Test
    public void testAmazonSpecificFields() {
        Version version = createVersion();
        Film film = createFilm(version);
        ItemAndVersion versionHierarchy = new ItemAndVersion(film, version);
        String versionCrid = "versionCrid";
        
        ProgramInformationType progInfo = generator.generate(versionHierarchy, versionCrid);
        
        assertEquals(versionCrid, progInfo.getProgramId());
        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:drrrrn", progInfo.getDerivedFrom().getCrid());
    }

    private static Film createFilm(Version version) {
        Film film = new Film();
        film.setId(35320383L);
        film.setCanonicalUri("http://v3.amazon.co.uk/amzn1.dv.gti.e2b8b2a4-94c7-a622-aa95-1af60936b0cd:GB");
        film.setPublisher(Publisher.AMAZON_V3);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("bbfc|15", Countries.GB)));
        film.setYear(1963);
        film.addAlias(new Alias("amazon:asin", "filmAsin"));
        
        film.setVersions(ImmutableSet.of(version));
        return film;
    }

    private static Version createVersion() {
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(90));
        return version;
    }
}
