package org.atlasapi.feeds.youview;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.apache.commons.io.IOUtils;
import org.atlasapi.feeds.tvanytime.DefaultTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.LoveFilmGroupInformationHierarchyTest.DummyContentResolver;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.intl.Countries;

public class BootstrapIntegrationTest {
    
    private static final YouViewGenreMapping genreMapping = new YouViewGenreMapping(); 
    private static final ProgramInformationGenerator progInfoGenerator = new LoveFilmProgramInformationGenerator();
    private static final GroupInformationGenerator groupInfoGenerator = new LoveFilmGroupInformationGenerator(genreMapping);
    private static final OnDemandLocationGenerator progLocationGenerator = new LoveFilmOnDemandLocationGenerator();
    private static DummyContentResolver contentResolver = new DummyContentResolver();
    
    private static final TvAnytimeGenerator generator = new DefaultTvAnytimeGenerator(
        progInfoGenerator, 
        groupInfoGenerator, 
        progLocationGenerator, 
        contentResolver,
        false
    );

    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private final YouViewUploader uploader = new YouViewUploader("youviewurl", generator, httpClient);
    private final YouViewDeleter deleter = new YouViewDeleter("youviewurl", httpClient);
    private DummyContentFinder contentFinder = new DummyContentFinder();
    private YouViewLastUpdatedStore store = new DummyLastUpdatedStore();
    
    private final YouViewUploadTask bootStrapUploader = new YouViewUploadTask(uploader, deleter, 5, contentFinder, store, true);

    @Test
    public void testBootstrapOutput() throws ValidityException, HttpException, ParsingException, IOException {
        Film film = createFilm("http://lovefilm.com/films/film1");
        
        Episode episode1 = createEpisode("http://lovefilm.com/episodes/episode1");
        Series series1 = createSeries("http://lovefilm.com/seasons/series1");
        Brand brand1 = createBrand("http://lovefilm.com/shows/brand1");
        episode1.setSeriesRef(ParentRef.parentRefFrom(series1));
        episode1.setEpisodeNumber(1);
        episode1.setSeriesNumber(1);
        episode1.setParentRef(ParentRef.parentRefFrom(brand1));
        series1.setChildRefs(ImmutableList.of(episode1.childRef()));
        series1.withSeriesNumber(1);
        series1.setParent(brand1);
        brand1.setSeriesRefs(ImmutableList.of(series1.childRef()));
        brand1.setChildRefs(ImmutableList.of(episode1.childRef()));
        
        Episode episode2 = createEpisode("http://lovefilm.com/episodes/episode2");
        Series series2 = createSeries("http://lovefilm.com/seasons/series2");
        Brand brand2 = createBrand("http://lovefilm.com/shows/brand2");
        episode2.setSeriesRef(ParentRef.parentRefFrom(series2));
        episode2.setEpisodeNumber(1);
        episode2.setSeriesNumber(2);
        episode2.setParentRef(ParentRef.parentRefFrom(brand2));
        series2.setChildRefs(ImmutableList.of(episode2.childRef()));
        series2.setParent(brand2);
        series2.withSeriesNumber(2);
        brand2.setSeriesRefs(ImmutableList.of(series2.childRef()));
        brand2.setChildRefs(ImmutableList.of(episode2.childRef()));
        
        Episode episode3 = createEpisode("http://lovefilm.com/episodes/episode3");
        Brand brand3 = createBrand("http://lovefilm.com/shows/brand3");
        episode3.setEpisodeNumber(1);
        episode3.setParentRef(ParentRef.parentRefFrom(brand3));
        brand3.setChildRefs(ImmutableList.of(episode3.childRef()));
        
        contentResolver.addContent(episode1);
        contentResolver.addContent(episode2);
        contentResolver.addContent(episode3);
        contentResolver.addContent(series1);
        contentResolver.addContent(series2);
        contentResolver.addContent(brand1);
        contentResolver.addContent(brand2);
        contentResolver.addContent(brand3);

        contentFinder.addContent(film);
        contentFinder.addContent(episode1);
        contentFinder.addContent(series2);
        contentFinder.addContent(brand3);
        
        bootStrapUploader.runTask();
        
        Mockito.verify(httpClient).post(Mockito.eq("youviewurl/transaction"), Mockito.eq(new StringPayload(fromXmlFile("youview-bootstrap.xml"))));
    }

    private String fromXmlFile(String fileName) throws ValidityException, ParsingException, IOException {
        URL testFile = Resources.getResource(getClass(), fileName);
        InputStream stream = Resources.newInputStreamSupplier(testFile).getInput();
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, Charsets.UTF_8.displayName());
        return writer.toString();
    }
    
    private Series createSeries(String uri) {
        Series series = new Series();
        
        populateContentFields(series, uri);
        series.setSpecialization(Specialization.TV);
        
        return series;
    }
    
    private Brand createBrand(String uri) {
        Brand brand = new Brand();
        
        populateContentFields(brand, uri);
        brand.setSpecialization(Specialization.TV);
        
        return brand;
    }
    
    private Episode createEpisode(String uri) {
        Episode episode = new Episode();
        
        populateItemFields(episode, uri);
        episode.setSpecialization(Specialization.TV);
        
        return episode;
    }

    private Film createFilm(String uri) {
        Film film = new Film();
        
        populateItemFields(film, uri);
        film.setSpecialization(Specialization.FILM);
        
        return film;
    }
    
    private void populateContentFields(Content content, String uri) {
        
        content.setCanonicalUri(uri);
        content.setCurie("lf:e-177221");
        content.setTitle("Dr. Strangelove");
        content.setDescription("The film is set at the height of the tensions between Russia and the United States");
        content.setGenres(ImmutableList.of("http://lovefilm.com/genres/comedy"));
        content.setPublisher(Publisher.LOVEFILM);
        content.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        content.setYear(1963);
        content.setLanguages(ImmutableList.of("en"));
        content.setMediaType(MediaType.VIDEO);
        
        CrewMember georgeScott = new CrewMember();
        georgeScott.withName("George C. Scott");
        
        CrewMember stanley = new CrewMember();
        stanley.withName("Stanley Kubrick");
        
        content.setPeople(ImmutableList.of(georgeScott, stanley));
    }
    
    private void populateItemFields(Item item, String uri) {
        
        populateContentFields(item, uri);
        
        item.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        
        Version version = new Version();
        Encoding encoding = new Encoding();
        Location location = new Location();
        Policy policy = new Policy();

        policy.setAvailabilityStart(new DateTime(2012, 7, 3, 0, 0, 0, DateTimeZone.UTC));
        policy.setAvailabilityEnd(new DateTime(2013, 7, 17, 0, 0, 0, DateTimeZone.UTC));
        
        location.setPolicy(policy);
        
        encoding.addAvailableAt(location);
        encoding.setVideoHorizontalSize(1280);
        encoding.setVideoVerticalSize(720);
        encoding.setVideoAspectRatio("16:9");
        encoding.setBitRate(3308);

        version.addManifestedAs(encoding);
        version.setDuration(Duration.standardMinutes(90));

        item.addVersion(version);
    }
    
    public static class DummyLastUpdatedStore implements YouViewLastUpdatedStore {
        
        DateTime lastUpdated = null;

        @Override
        public Optional<DateTime> getLastUpdated() {
            return Optional.fromNullable(lastUpdated);
        }

        @Override
        public void setLastUpdated(DateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
        }
        
    }
    
    public static class DummyContentFinder implements LastUpdatedContentFinder {

        private final List<Content> data = Lists.newArrayList();
        
        public void addContent(Content content) {
            data.add(content);
        }
        
        @Override
        public Iterator<Content> updatedSince(final Publisher publisher, DateTime since) {
            return Iterables.filter(data, new Predicate<Content>() {
               @Override
               public boolean apply(Content input) {
                   return publisher.equals(input.getPublisher());
               }
            }).iterator();
        }
        
    }
}
