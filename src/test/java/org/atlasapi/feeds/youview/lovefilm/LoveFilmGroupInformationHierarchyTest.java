package org.atlasapi.feeds.youview.lovefilm;

import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.tvanytime.JaxbTvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementCreator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.ContentResolvingContentHierarchyExtractor;
import org.atlasapi.feeds.youview.UriBasedContentPermit;
import org.atlasapi.feeds.youview.genres.GenreMapping;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.ChildRef;
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
import org.atlasapi.media.entity.SortKey;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.ResolvedContent.ResolvedContentBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Ignore;
import org.junit.Test;

import tva.metadata._2010.BaseMemberOfType;
import tva.metadata._2010.BasicContentDescriptionType;
import tva.metadata._2010.GroupInformationTableType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.ProgramGroupTypeType;
import tva.metadata._2010.RelatedMaterialType;
import tva.metadata._2010.TVAMainType;
import tva.mpeg7._2008.MediaLocatorType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.metabroadcast.common.intl.Countries;

public class LoveFilmGroupInformationHierarchyTest {

    private IdGenerator idGenerator = new LoveFilmIdGenerator();
    private TvAnytimeElementFactory elementFactory = TvAnytimeElementFactory.INSTANCE;
    private ProgramInformationGenerator progInfoGenerator = new LoveFilmProgramInformationGenerator(idGenerator, elementFactory);
    private GenreMapping genreMapping = new LoveFilmGenreMapping();
    private GroupInformationGenerator groupInfoGenerator = new LoveFilmGroupInformationGenerator(idGenerator, genreMapping);
    private OnDemandLocationGenerator progLocationGenerator = new LoveFilmOnDemandLocationGenerator(idGenerator);
    private DummyContentResolver contentResolver = new DummyContentResolver();
    private ContentHierarchyExtractor hierarchy = new ContentResolvingContentHierarchyExtractor(contentResolver);
    private TvAnytimeElementCreator elementCreator = new LoveFilmTvAnytimeElementCreator(
            progInfoGenerator, 
            groupInfoGenerator, 
            progLocationGenerator, 
            hierarchy,
            new UriBasedContentPermit()
    );
    
    private final TvAnytimeGenerator generator = new JaxbTvAnytimeGenerator(elementCreator);
    
    // TODO how should this case be handled? where should ASIN checking logic sit?
    @Ignore
    @Test
    public void testSkipsItemIfNoAsin() throws JAXBException {
        Film film = createFilm("FilmUri");
        film.setImage("Film Image");
        
        TVAMainType tvaMain = generateTVA(film);
        
        GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();
        
        assert(groupInfoTable.getGroupInformation().isEmpty());
    }
    
    @Test
    public void testFilmGeneration() throws JAXBException {
        Film film = createFilm("FilmUri");
        film.setImage("Film Image");
        film.addAlias(new Alias("gb:amazon:asin", "123456"));
        
        TVAMainType tvaMain = generateTVA(film);
        
        GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();
        
        GroupInformationType groupInfo = Iterables.getOnlyElement(groupInfoTable.getGroupInformation());
        
        assertEquals("Film Image", getImage(groupInfo));
        assertEquals("crid://lovefilm.com/product/FilmUri", groupInfo.getGroupId());
    }
    
    @Test
    public void testBrandGeneration() throws JAXBException {
        Brand brand = createBrand("BrandUri");
        brand.setImage("Brand Image");
        brand.addAlias(new Alias("gb:amazon:asin", "123456"));
        
        Episode episode = createEpisode("Episode Uri");
        episode.setImage("Episode Image");
        brand.setChildRefs(ImmutableList.of(episode.childRef()));

        contentResolver.addContent(episode);
        
       TVAMainType tvaMain = generateTVA(brand);
       
       GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();
       
       GroupInformationType groupInfo = Iterables.getOnlyElement(groupInfoTable.getGroupInformation());
       
       assertEquals("Episode Image", getImage(groupInfo));
       assertEquals("crid://lovefilm.com/product/BrandUri", groupInfo.getGroupId());
    }
    
    @Test
    public void testSeriesGeneration() throws JAXBException {
        Series series = createSeries("SeriesUri");
        series.setImage("Series Image");
        series.addAlias(new Alias("gb:amazon:asin", "123456"));

        Episode episode = createEpisode("Episode Uri");
        episode.setImage("Episode Image");
        series.setChildRefs(ImmutableList.of(episode.childRef()));

        contentResolver.addContent(episode);

        TVAMainType tvaMain = generateTVA(series);

        GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();

        GroupInformationType groupInfo = Iterables.getOnlyElement(groupInfoTable.getGroupInformation());

        assertEquals("Episode Image", getImage(groupInfo));
        assertEquals("crid://lovefilm.com/product/SeriesUri", groupInfo.getGroupId());
    }
    
    @Test
    public void testSeriesGenerationWithBrand() throws JAXBException {
        Series series = createSeries("SeriesUri");
        series.setImage("Series Image");
        series.addAlias(new Alias("gb:amazon:asin", "seriesAsin"));
        
        Brand brand = createBrand("BrandUri");
        brand.setImage("Brand Image");
        brand.addAlias(new Alias("gb:amazon:asin", "brandAsin"));
        
        Episode episode = createEpisode("EpisodeUri");
        episode.setImage("Episode Image");
        
        series.setChildRefs(ImmutableList.of(episode.childRef()));
        series.setParent(brand);
        
        brand.setChildRefs(ImmutableList.of(episode.childRef()));
        brand.setSeriesRefs(ImmutableList.of(series.seriesRef()));

        contentResolver.addContent(episode);
        contentResolver.addContent(brand);
        
       TVAMainType tvaMain = generateTVA(series);
       
       GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();
       
       GroupInformationType firstGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 0);
       GroupInformationType secondGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 1);
       
       assertEquals("Episode Image", getImage(firstGroupInfo));
       assertEquals("Episode Image", getImage(secondGroupInfo));
       
       BaseMemberOfType memberOf = null;
       if (firstGroupInfo.getMemberOf().isEmpty()) {
           memberOf = Iterables.getOnlyElement(secondGroupInfo.getMemberOf());
           assertEquals("crid://lovefilm.com/product/SeriesUri", secondGroupInfo.getGroupId());
       } else {
           memberOf = Iterables.getOnlyElement(firstGroupInfo.getMemberOf());
           assertEquals("crid://lovefilm.com/product/SeriesUri", firstGroupInfo.getGroupId());
       }
       
       assertEquals("crid://lovefilm.com/product/BrandUri", memberOf.getCrid());
    }
    
    @Test
    public void testNonFirstSeriesGenerationWithBrand() throws JAXBException {
        Series series1 = createSeries("Series 1 Uri");
        series1.setImage("Series 1 Image");
        
        Series series2 = createSeries("Series2Uri");
        series2.setImage("Series 2 Image");
        series2.addAlias(new Alias("gb:amazon:asin", "series2Asin"));
        
        Brand brand = createBrand("BrandUri");
        brand.setImage("Brand Image");
        brand.addAlias(new Alias("gb:amazon:asin", "brandAsin"));
        
        Episode episode1S1 = createEpisode("Episode 1 Series 1 Uri");
        episode1S1.setImage("Episode 1 Series 1 Image");
        episode1S1.setEpisodeNumber(1);
        episode1S1.setSeriesNumber(1);
        episode1S1.setSeries(series1);
        episode1S1.setContainer(brand);
        
        Episode episode1S2 = createEpisode("Episode 1 Series 2 Uri");
        episode1S2.setImage("Episode 1 Series 2 Image");
        episode1S2.setEpisodeNumber(1);
        episode1S2.setSeriesNumber(2);
        episode1S2.setSeries(series2);
        episode1S2.setContainer(brand);

        series1.setChildRefs(ImmutableList.of(episode1S1.childRef()));
        series1.setParent(brand);
        
        series2.setChildRefs(ImmutableList.of(episode1S2.childRef()));
        series2.setParent(brand);
        
        Map<String, ChildRef> sortedMap = ImmutableSortedMap
            .<String, ChildRef>orderedBy(new SortKey.SortKeyOutputComparator())
            .put(SortKey.keyFrom(episode1S1), episode1S1.childRef())
            .put(SortKey.keyFrom(episode1S2), episode1S2.childRef())
            .build();
        
        brand.setChildRefs(sortedMap.values());
        brand.setSeriesRefs(ImmutableList.of(series1.seriesRef(), series2.seriesRef()));

        contentResolver.addContent(episode1S1);
        contentResolver.addContent(episode1S2);
        contentResolver.addContent(brand);
        
       TVAMainType tvaMain = generateTVA(series2);
       
       GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();
       
       GroupInformationType firstGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 0);
       GroupInformationType secondGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 1);
       // check that series == ep 1 s2
       // and brand == ep 1 s1
       if (isSeries(firstGroupInfo)) {
           assertEquals("Episode 1 Series 2 Image", getImage(firstGroupInfo));
           assertEquals("Episode 1 Series 1 Image", getImage(secondGroupInfo));
       } else {
           assertEquals("Episode 1 Series 1 Image", getImage(firstGroupInfo));
           assertEquals("Episode 1 Series 2 Image", getImage(secondGroupInfo));
       }
       
       BaseMemberOfType memberOf = null;
       if (firstGroupInfo.getMemberOf().isEmpty()) {
           memberOf = Iterables.getOnlyElement(secondGroupInfo.getMemberOf());
           assertEquals("crid://lovefilm.com/product/Series2Uri", secondGroupInfo.getGroupId());
       } else {
           memberOf = Iterables.getOnlyElement(firstGroupInfo.getMemberOf());
           assertEquals("crid://lovefilm.com/product/Series2Uri", firstGroupInfo.getGroupId());
       }
       
       assertEquals("crid://lovefilm.com/product/BrandUri", memberOf.getCrid());
    }
    
    @Test
    public void testEpisodeGenerationNoSeriesNoBrand() throws JAXBException {
        Episode episode = createEpisode("EpisodeUri");
        episode.setImage("Episode Image");
        episode.addAlias(new Alias("gb:amazon:asin", "123456"));

        TVAMainType tvaMain = generateTVA(episode);

        GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();

        GroupInformationType groupInfo = Iterables.getOnlyElement(groupInfoTable.getGroupInformation());

        assertEquals("Episode Image", getImage(groupInfo));
        
        assertEquals("crid://lovefilm.com/product/EpisodeUri", groupInfo.getGroupId());
    }
    
    @Test
    public void testEpisodeGenerationNoBrand() throws JAXBException {
        Series series = createSeries("SeriesUri");
        series.setImage("Series Image");
        series.addAlias(new Alias("gb:amazon:asin", "seriesAsin"));
        
        Episode episode = createEpisode("EpisodeUri");
        episode.setImage("Episode Image");
        episode.setEpisodeNumber(1);
        episode.setSeriesRef(ParentRef.parentRefFrom(series));
        episode.addAlias(new Alias("gb:amazon:asin", "episodeAsin"));
        
        series.setChildRefs(ImmutableList.of(episode.childRef()));
        
        contentResolver.addContent(series);
        contentResolver.addContent(episode);

        TVAMainType tvaMain = generateTVA(episode);

        GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();

        GroupInformationType firstGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 0);
        GroupInformationType secondGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 1);
        
        assertEquals("Episode Image", getImage(firstGroupInfo));
        assertEquals("Episode Image", getImage(secondGroupInfo));
        
        BaseMemberOfType memberOf = null;
        if (firstGroupInfo.getMemberOf().isEmpty()) {
            memberOf = Iterables.getOnlyElement(secondGroupInfo.getMemberOf());
            assertEquals("crid://lovefilm.com/product/EpisodeUri", secondGroupInfo.getGroupId());
        } else {
            memberOf = Iterables.getOnlyElement(firstGroupInfo.getMemberOf());
            assertEquals("crid://lovefilm.com/product/EpisodeUri", firstGroupInfo.getGroupId());
        }
        
        assertEquals("crid://lovefilm.com/product/SeriesUri", memberOf.getCrid());
    }
    
    @Test
    public void testEpisodeGenerationNoSeries() throws JAXBException {
        Brand brand = createBrand("BrandUri");
        brand.setImage("Brand Image");
        brand.addAlias(new Alias("gb:amazon:asin", "brandAsin"));
        
        Episode episode = createEpisode("EpisodeNoSeriesUri");
        episode.setImage("Episode No Series Image");
        episode.setContainer(brand);
        episode.setEpisodeNumber(1);
        episode.addAlias(new Alias("gb:amazon:asin", "episodeAsin"));

        brand.setChildRefs(ImmutableList.of(episode.childRef()));
        
        contentResolver.addContent(episode);
        contentResolver.addContent(brand);

        TVAMainType tvaMain = generateTVA(episode);
        
        GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();

        GroupInformationType firstGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 0);
        GroupInformationType secondGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 1);
        
        assertEquals("Episode No Series Image", getImage(firstGroupInfo));
        assertEquals("Episode No Series Image", getImage(secondGroupInfo));
        
        BaseMemberOfType memberOf = null;
        if (firstGroupInfo.getMemberOf().isEmpty()) {
            memberOf = Iterables.getOnlyElement(secondGroupInfo.getMemberOf());
            assertEquals("crid://lovefilm.com/product/EpisodeNoSeriesUri", secondGroupInfo.getGroupId());
        } else {
            memberOf = Iterables.getOnlyElement(firstGroupInfo.getMemberOf());
            assertEquals("crid://lovefilm.com/product/EpisodeNoSeriesUri", firstGroupInfo.getGroupId());
        }
        
        assertEquals("crid://lovefilm.com/product/BrandUri", memberOf.getCrid());
    }
    
    @Test
    public void testEpisodeGeneration() throws JAXBException {
        Brand brand = createBrand("BrandUri");
        brand.setImage("Brand Image");
        brand.addAlias(new Alias("gb:amazon:asin", "brandAsin"));
        
        Series series = createSeries("SeriesUri");
        series.setImage("Series Image");
        series.addAlias(new Alias("gb:amazon:asin", "seriesAsin"));
        
        Episode episode = createEpisode("EpisodeUri");
        episode.setImage("Episode Image");
        episode.setSeriesRef(ParentRef.parentRefFrom(series));
        episode.setContainer(brand);
        episode.setEpisodeNumber(1);
        episode.addAlias(new Alias("gb:amazon:asin", "episodeAsin"));

        brand.setChildRefs(ImmutableList.of(episode.childRef()));
        brand.setSeriesRefs(ImmutableList.of(series.seriesRef()));
        series.setChildRefs(ImmutableList.of(episode.childRef()));
        series.setParent(brand);
        
        contentResolver.addContent(episode);
        contentResolver.addContent(series);
        contentResolver.addContent(brand);

        TVAMainType tvaMain = generateTVA(episode);

        GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();

        GroupInformationType firstGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 0);
        GroupInformationType secondGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 1);
        GroupInformationType thirdGroupInfo = Iterables.get(groupInfoTable.getGroupInformation(), 2);
        
        assertEquals("Episode Image", getImage(firstGroupInfo));
        assertEquals("Episode Image", getImage(secondGroupInfo));
        assertEquals("Episode Image", getImage(thirdGroupInfo));
        
        if (firstGroupInfo.getMemberOf().isEmpty()) {
            assertThat(Iterables.getOnlyElement(secondGroupInfo.getMemberOf()).getCrid(), isOneOf("crid://lovefilm.com/product/seriesUri", "crid://lovefilm.com/product/brandUri"));
            assertThat(Iterables.getOnlyElement(thirdGroupInfo.getMemberOf()).getCrid(), isOneOf("crid://lovefilm.com/product/seriesUri", "crid://lovefilm.com/product/brandUri"));
            assertEquals("crid://lovefilm.com/product/BrandUri", firstGroupInfo.getGroupId());
        } else if (secondGroupInfo.getMemberOf().isEmpty()) {
            assertThat(Iterables.getOnlyElement(firstGroupInfo.getMemberOf()).getCrid(), isOneOf("crid://lovefilm.com/product/SeriesUri", "crid://lovefilm.com/product/BrandUri"));
            assertThat(Iterables.getOnlyElement(thirdGroupInfo.getMemberOf()).getCrid(), isOneOf("crid://lovefilm.com/product/SeriesUri", "crid://lovefilm.com/product/BrandUri"));
            assertEquals("crid://lovefilm.com/product/BrandUri", secondGroupInfo.getGroupId());
        } else {
            assertThat(Iterables.getOnlyElement(firstGroupInfo.getMemberOf()).getCrid(), isOneOf("crid://lovefilm.com/product/SeriesUri", "crid://lovefilm.com/product/BrandUri"));
            assertThat(Iterables.getOnlyElement(secondGroupInfo.getMemberOf()).getCrid(), isOneOf("crid://lovefilm.com/product/SeriesUri", "crid://lovefilm.com/product/BrandUri"));
            assertEquals("crid://lovefilm.com/product/BrandUri", thirdGroupInfo.getGroupId());
        }
    }
    
    @Test
    public void testEpisodeGenerationFromNonFirstSeries() throws JAXBException {
        Series series1 = createSeries("Series 1 Uri");
        series1.withSeriesNumber(1);
        series1.setImage("Series 1 Image");
        series1.addAlias(new Alias("gb:amazon:asin", "series1Asin"));
        
        Series series2 = createSeries("Series 2 Uri");
        series2.withSeriesNumber(2);
        series2.setImage("Series 2 Image");
        series2.addAlias(new Alias("gb:amazon:asin", "series2Asin"));
        
        Brand brand = createBrand("Brand Uri");
        brand.setImage("Brand Image");
        brand.addAlias(new Alias("gb:amazon:asin", "brandAsin"));

        Episode episode1S1 = createEpisode("Episode 1 Series 1 Uri");
        episode1S1.setImage("Episode 1 Series 1 Image");
        episode1S1.setEpisodeNumber(1);
        episode1S1.setSeriesNumber(1);
        episode1S1.setSeries(series1);
        episode1S1.setContainer(brand);
        episode1S1.addAlias(new Alias("gb:amazon:asin", "episode1S1Asin"));

        Episode episode1S2 = createEpisode("Episode 1 Series 2 Uri");
        episode1S2.setImage("Episode 1 Series 2 Image");
        episode1S2.setEpisodeNumber(1);
        episode1S2.setSeriesNumber(2);
        episode1S2.setSeries(series2);
        episode1S2.setContainer(brand);
        episode1S2.addAlias(new Alias("gb:amazon:asin", "episode1S2Asin"));

        series1.setChildRefs(ImmutableList.of(episode1S1.childRef()));
        series1.setParent(brand);

        series2.setChildRefs(ImmutableList.of(episode1S2.childRef()));
        series2.setParent(brand);

        Map<String, ChildRef> sortedMap = ImmutableSortedMap
                .<String, ChildRef>orderedBy(new SortKey.SortKeyOutputComparator())
                .put(SortKey.keyFrom(episode1S1), episode1S1.childRef())
                .put(SortKey.keyFrom(episode1S2), episode1S2.childRef())
                .build();

        brand.setChildRefs(sortedMap.values());
        brand.setSeriesRefs(ImmutableList.of(series1.seriesRef(), series2.seriesRef()));

        contentResolver.addContent(episode1S1);
        contentResolver.addContent(episode1S2);
        contentResolver.addContent(series1);
        contentResolver.addContent(series2);
        contentResolver.addContent(brand);

        TVAMainType tvaMain = generateTVA(episode1S2);

        GroupInformationTableType groupInfoTable = tvaMain.getProgramDescription().getGroupInformationTable();

        // episode == ep 1 s 2
        // check that series == ep 1 s 2
        // and brand == ep 1 s 1
        boolean foundEpisode = false;
        boolean foundSeries = false;
        boolean foundBrand = false;
        for (GroupInformationType groupInfo : groupInfoTable.getGroupInformation()) {
            if (isSeries(groupInfo)) {
                assertEquals("Episode 1 Series 2 Image", getImage(groupInfo));
                foundSeries = true;
            } else if (isBrand(groupInfo)) {
                assertEquals("Episode 1 Series 1 Image", getImage(groupInfo));
                foundBrand = true;
            } else {
                assertEquals("Episode 1 Series 2 Image", getImage(groupInfo));
                foundEpisode = true;
            }
        }

        assertTrue(foundEpisode);
        assertTrue(foundSeries);
        assertTrue(foundBrand);
    }


    private boolean isBrand(GroupInformationType groupInfo) {
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        return groupType.getValue().equals("show");
    }

    private boolean isSeries(GroupInformationType groupInfo) {
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        return groupType.getValue().equals("series");
    }

    private TVAMainType generateTVA(Content content) {
        return generator.generateTVAnytimeFrom(content).getValue();
    }
    
    private String getImage(GroupInformationType groupInfo) {
        BasicContentDescriptionType basicDescription = groupInfo.getBasicDescription();
        RelatedMaterialType relatedMaterial = Iterables.getOnlyElement(basicDescription.getRelatedMaterial());
        MediaLocatorType locator = relatedMaterial.getMediaLocator();
        return locator.getMediaUri();   
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

    public static class DummyContentResolver implements ContentResolver {
        
        private final Map<String, Content> data = Maps.newHashMap();
        
        public void addContent(Content content) {
            data.put(content.getCanonicalUri(), content);
        }
        
        @Override
        public ResolvedContent findByCanonicalUris(Iterable<String> uris) {
            ResolvedContentBuilder builder = ResolvedContent.builder();
            for (String uri : uris) {
                builder.put(uri, data.get(uri));
            }
            return builder.build();
        }

        @Override
        public ResolvedContent findByUris(Iterable<String> uris) {
            throw new UnsupportedOperationException();
        }
    };
}
