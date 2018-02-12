package org.atlasapi.feeds.youview.unbox;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.youview.AmazonContentConsolidator;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;

import com.metabroadcast.common.intl.Countries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.joda.time.Duration;
import org.junit.Test;
import tva.metadata._2010.ProgramInformationType;
import tva.metadata.extended._2010.ExtendedContentDescriptionType;
import tva.mpeg7._2008.UniqueIDType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnboxProgramInformationGeneratorTest extends org.atlasapi.TestsWithConfiguration {

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
        assertEquals(UnboxOnDemandLocationGenerator.UNBOX_DEEP_LINKING_ID, otherId.getAuthority());
        assertEquals("crid://amazon.com/stage-metabroadcast.com/content/drrrrn", progInfo.getDerivedFrom().getCrid());
    }

    @Test
    public void testVersionConsolidation() {
        Location locA = new Location();
        locA.setUri("LocationA");
        Location locB = new Location();
        locB.setUri("LocationB");
        Encoding enc1 = new Encoding();
        enc1.setVideoHorizontalSize(1000);
        enc1.setCanonicalUri("Encoding1-1000");
        enc1.setAvailableAt(new HashSet<Location>(Arrays.asList(locA, locB)));

        Version version1 = createVersion();
        version1.setCanonicalUri("Version1");
        version1.setManifestedAs(new HashSet<Encoding>(Arrays.asList(enc1)));

        Location locC = new Location();
        locC.setUri("LocationC");
        Location locD = new Location();
        locD.setUri("LocationD");
        Encoding enc2 = new Encoding();
        enc2.setVideoHorizontalSize(1000);
        enc2.setCanonicalUri("Encoding2-1000");
        enc2.setAvailableAt(new HashSet<Location>(Arrays.asList(locC, locD)));

        Location locE = new Location();
        locE.setUri("LocationE");
        Location locF = new Location();
        locF.setUri("LocationF");
        Encoding enc3 = new Encoding();
        enc3.setVideoHorizontalSize(2000);
        enc3.setCanonicalUri("Encoding3-2000");
        enc3.setAvailableAt(new HashSet<Location>(Arrays.asList(locE, locF)));

        Version version2 = createVersion();
        version2.setCanonicalUri("Version2");
        version2.setManifestedAs(new HashSet<Encoding>(Arrays.asList(enc2, enc3)));

        Film film = new Film();
        film.setId(1L);
        film.setCanonicalUri("FilmUri");
        film.setPublisher(Publisher.AMAZON_UNBOX);

        film.setVersions(ImmutableSet.of(version1, version2));

        Content consolidated = AmazonContentConsolidator.consolidate(film);

        //TESTS

        assertEquals(
                "Consolidation should not leave more than 1 versions, or eat them all.",
                consolidated.getVersions().size(), 1
        );
        Iterator<Version> vIter = consolidated.getVersions().iterator();
        Version version = vIter.next();
        assertEquals(
                "Version should end up with one Encoding for each quality",
                version.getManifestedAs().size(), 2
        );

        //find the 1000 quality encoding.
        Encoding encoding1000 = null;
        for (Encoding encoding : version.getManifestedAs()) {
            if(encoding.getVideoHorizontalSize() == 1000){
                encoding1000 = encoding;
            }
        }
        assertEquals(
                encoding1000.getCanonicalUri() + " should contain the 4 merged locations.",
                encoding1000.getAvailableAt().size(), 4
        );
        Set<String> acceptableUris = new HashSet<String>(Arrays.asList(
                "LocationA", "LocationB", "LocationC", "LocationD"
        ));
        for (Location location : encoding1000.getAvailableAt()) {
            assertTrue(
                    location.getUri() + " should not be in " + encoding1000.getCanonicalUri(),
                    acceptableUris.contains(location.getUri())
            );
        }


        //find the 2000 quality encoding.
        Encoding encoding2000 = null;
        for (Encoding encoding : version.getManifestedAs()) {
            if(encoding.getVideoHorizontalSize() == 2000){
                encoding2000 = encoding;
            }
        }
        assertEquals(
                encoding2000.getCanonicalUri() + " should contain the 2 transferred locations.",
                encoding2000.getAvailableAt().size(), 2
        );

        acceptableUris = new HashSet<String>(Arrays.asList(
                "LocationE", "LocationF"
        ));
        for (Location location : encoding2000.getAvailableAt()) {
            assertTrue(
                    location.getUri() + " should not be in " + encoding2000.getCanonicalUri(),
                    acceptableUris.contains(location.getUri())
            );
        }


    }

    private Film createFilm(Version version) {
        Film film = new Film();
        film.setId(35320383L);
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
