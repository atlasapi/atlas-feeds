package org.atlasapi.feeds.youview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.intl.Countries;

public class IntegrationTest {
    
    private static final YouViewGenreMapping genreMapping = new YouViewGenreMapping(); 
    private static final ProgramInformationGenerator progInfoGenerator = new LoveFilmProgramInformationGenerator();
    private static final GroupInformationGenerator groupInfoGenerator = new LoveFilmGroupInformationGenerator(genreMapping);
    private static final OnDemandLocationGenerator progLocationGenerator = new LoveFilmOnDemandLocationGenerator();
    
    private static final TvAnytimeGenerator generator = new DefaultTvAnytimeGenerator(
        progInfoGenerator, 
        groupInfoGenerator, 
        progLocationGenerator, 
        Mockito.mock(ContentResolver.class),
        false
    );

    @Test
    @Ignore
    public void testXmlOutput() throws FileNotFoundException {
        File testFile = new File("src/test/resources/org/atlasapi/feeds/youview", "xml_test.xml");
        OutputStream out = new FileOutputStream(testFile);
        
        generator.generateXml(ImmutableList.<Content>of(createFilm()), out);
    }

    private Film createFilm() {
        Film film = new Film();
        
        film.setCanonicalUri("http://lovefilm.com/films/177221");
        film.setCurie("lf:f-177221");
        film.setTitle("Dr. Strangelove");
        film.setDescription("The film is set at the height of the tensions between Russia and the United States");
        film.setGenres(ImmutableList.of("http://lovefilm.com/genres/comedy"));
        film.setImage("http://www.lovefilm.com/lovefilm/images/products/heroshots/1/177221-large.jpg");
        film.setPublisher(Publisher.LOVEFILM);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.setLanguages(ImmutableList.of("en"));
        film.setMediaType(MediaType.VIDEO);
        film.setSpecialization(Specialization.FILM);
        
        CrewMember georgeScott = new CrewMember();
        georgeScott.withName("George C. Scott");
        CrewMember stanley = new CrewMember();
        stanley.withName("Stanley Kubrick");
        CrewMember peter = new CrewMember();
        peter.withName("Peter Sellers");
        film.setPeople(ImmutableList.of(georgeScott, stanley, peter));
        
        Version version = new Version();
        Encoding encoding = new Encoding();
        Location location = new Location();
        Policy policy = new Policy();

        policy.setAvailabilityStart(new DateTime(2012, 7, 3, 0, 0, 0, DateTimeZone.UTC));
        policy.setAvailabilityEnd(new DateTime(2013, 7, 17, 0, 0, 0, DateTimeZone.UTC));
        
        location.setPolicy(policy);
        
        encoding.addAvailableAt(location);
        encoding.addAvailableAt(location);
        encoding.setVideoHorizontalSize(1280);
        encoding.setVideoVerticalSize(720);
        encoding.setVideoAspectRatio("16:9");
        encoding.setBitRate(3308);
        
        version.addManifestedAs(encoding);
        version.setDuration(Duration.standardMinutes(90));
        
        film.addVersion(version);
        
        return film;
    }
}
