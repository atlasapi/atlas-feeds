package org.atlasapi.feeds.lakeview;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.base.Maybe;

public class LakeviewFeedCompilerTest {

    private static final String CHANNEL_KEY = "channel-key";
    private static final Channel CHANNEL = new Channel(Publisher.METABROADCAST, "Channel", 
                                                       CHANNEL_KEY, true, MediaType.VIDEO, "http://example.org");
    
    private static final XMLNamespace LAKEVIEW = new XMLNamespace("", "http://schemas.microsoft.com/Lakeview/2013/07/01/ingestion");
    private ChannelResolver channelResolver = mock(ChannelResolver.class);
    
    private final LakeviewFeedCompiler feedCompiler = new LakeviewFeedCompiler(channelResolver, false, true);
    private final LakeviewFeedCompiler genericTitlesFeedCompiler = new LakeviewFeedCompiler(channelResolver, true, true);
    
    @Before
    public void setUp() {
        when(channelResolver.fromKey(CHANNEL_KEY)).thenReturn(Maybe.just(CHANNEL));
    }
    
	@Test
	public void testBrandAtomUri() {
		assertEquals("https://xbox.channel4.com/pmlsd/educating-essex/4od.atom", 
				feedCompiler.brandAtomUri("tag:pmlsc.channel4.com,2009:/programmes/educating-essex"));
	}
	
	@Test
	public void testSeriesAtomUri() {
		assertEquals("https://xbox.channel4.com/pmlsd/educating-essex/4od.atom#series-1", 
				feedCompiler.seriesAtomUri("tag:pmlsc.channel4.com,2009:/programmes/educating-essex/episode-guide/series-1"));
	}
	
	@Test
	public void testNonGenericSeriesTitleGenerated() {
        String seriesTitle = "Lord of the Wrongs";
	    Element element = getSeriesElement(seriesTitle, 12, false);
	    Elements titles = element.getChildElements("Title", LAKEVIEW.getUri());
	    
	    assertEquals("Should have exactly one title", 1, titles.size());
	    assertEquals("Title should be as set", seriesTitle, titles.get(0).getValue());
	}

	@Test
	public void testGenericSeriesTitleGenerated() {
	    LakeviewFeedCompiler compiler = new LakeviewFeedCompiler(channelResolver, true, true);
	    String seriesTitle = "Lord of the Wrongs";
	    Element element = getSeriesElement(seriesTitle, 12, true, compiler);
	    Elements titles = element.getChildElements("Title", LAKEVIEW.getUri());

	    assertEquals("Should have exactly one title", 1, titles.size());
	    assertEquals("Title should be generic", "Series 12", titles.get(0).getValue());
	}

	private Element getSeriesElement(String seriesTitle, int seriesNumber, boolean genericTitleEnabled) {
	    return getSeriesElement(seriesTitle, seriesNumber, genericTitleEnabled, feedCompiler);
	}
	
	private Element getSeriesElement(String seriesTitle, int seriesNumber, boolean genericTitleEnabled,
	        LakeviewFeedCompiler feedCompiler) {
        
        Series series = new Series("seriesUri", "seriesCurie", null);
        series.addAliasUrl("tag:pmlsc.channel4.com,2009:/programmes/brand/episode-guide/series-1");
        Brand parent = new Brand("brandUri", "brandCurie", null);
        parent.addAliasUrl("tag:pmlsc.channel4.com,2009:/programmes/brand");
        series.setTitle(seriesTitle);
        series.withSeriesNumber(seriesNumber);
        
        return feedCompiler.createSeriesElem(series, parent, new DateTime(), null);
	}
	
	@Test
	public void testNonGenericEpisodeTitleGenerated() {
	    String episodeTitle="The One That Was Funny";
	    
        Element element = createEpisodeElement(episodeTitle, 3012, false);
        Elements titles = element.getChildElements("Title", LAKEVIEW.getUri());
        
        assertEquals("Should have exactly one title", 1, titles.size());
        assertEquals("Title should be as set", episodeTitle, titles.get(0).getValue());
	}

	@Test
	public void testGenericEpisodeTitleGenerated() {
	    String episodeTitle="The One That Was Funny";

	    Element element = createEpisodeElement(episodeTitle, 3012, true);
	    Elements titles = element.getChildElements("Title", LAKEVIEW.getUri());

	    assertEquals("Should have exactly one title", 1, titles.size());
	    assertEquals("Title should be generic", "Episode 3012", titles.get(0).getValue());
	}
	
	private Brand createBrand() {
        Brand brand = new Brand("brandUri", "brandCurie", null);
        brand.setPresentationChannel(CHANNEL);
        brand.addAliasUrl("tag:pmlsc.channel4.com,2009:/programmes/brand");
        return brand;
	}
	
	private Series createSeries(Brand brand) {
	    Series series = new Series("seriesUri", "seriesCurie", null);
        series.addAliasUrl("tag:pmlsc.channel4.com,2009:/programmes/brand/episode-guide/series-1");
        series.setParent(brand);
        return series;
	}

	private Episode createEpisode(String episodeTitle, int episodeNumber, boolean genericTitleEnabled, Brand brand, Series series) {
	    Episode episode = new Episode(String.format("http://www.channel4.com/programmes/hierarchical-uri/episode-guide/series-1/episode-%d", episodeNumber), "episodeCurie", null);
        episode.addAliasUrl(String.format("tag:pmlsc.channel4.com,2009:/programmes/hierarchical-uri/episode-guide/series-1/episode-%d", episodeNumber));

        episode.setTitle(episodeTitle);
        episode.setEpisodeNumber(episodeNumber);
        episode.setContainer(brand);
        
        Version version = new Version();
        Encoding encoding = new Encoding();
        Location location = new Location();
        location.setPolicy(new Policy()
                    .withPlatform(Platform.XBOX)
                    .withAvailabilityStart(new DateTime())
                    .withAvailabilityEnd(new DateTime()));
        location.setTransportType(TransportType.APPLICATION);
        
        encoding.setAvailableAt(ImmutableSet.of(location));
        version.setManifestedAs(ImmutableSet.of(encoding));
        episode.setVersions(ImmutableSet.of(version));

        return episode;
	}
	
    private Element createEpisodeElement(String episodeTitle, int episodeNumber, boolean genericTitleEnabled) {
        Brand brand = createBrand();
        Series series = createSeries(brand);
        Episode episode = createEpisode(episodeTitle, episodeNumber, genericTitleEnabled, brand, series);
        if (genericTitleEnabled) {
            return genericTitlesFeedCompiler.createEpisodeElem(episode, brand, series, new DateTime(), null);
        } else {
            return feedCompiler.createEpisodeElem(episode, brand, series, new DateTime(), null);
        }
    }
    
    @Test
    public void testGeneratesCorrectIdsForProgrammeIdUri() {
        
        Episode episode = new Episode();
        episode.addAlias(new Alias("gb:channel4:prod:pmlsd:programmeId", "555/555"));
        episode.setCanonicalUri("http://www.channel4.com/programmes/55103/175");
        episode.addAliasUrl("tag:http://www.channel4.com/programmes/hollyoaks/episode-guide/series-22/episode-175");
        episode.setTitle("Title");
        episode.setEpisodeNumber(3);
        
        Brand container = new Brand("brandUri", "brandCurie", null);
        container.addAliasUrl("tag:http://www.channel4.com/programmes/hollyoaks");
        
        Version version = new Version();
        Encoding encoding = new Encoding();
        Location location = new Location();
        location.setUri("https://ais.channel4.com/asset/3567007");
        location.setPolicy(new Policy()
                    .withPlatform(Platform.XBOX)
                    .withAvailabilityStart(new DateTime())
                    .withAvailabilityEnd(new DateTime()));
        location.setTransportType(TransportType.LINK);
        
        encoding.setAvailableAt(ImmutableSet.of(location));
        version.setManifestedAs(ImmutableSet.of(encoding));
        episode.setVersions(ImmutableSet.of(version));
        episode.setContainer(container);

        Series series = new Series("seriesUri", "seriesCurie", null);
        episode.setSeries(series);
        series.addAliasUrl("tag:http://www.channel4.com/programmes/xxx/episode-guide/9");
       

        Element elem = feedCompiler.createEpisodeElem(episode, container, series, new DateTime(), null);

        assertEquals("http://channel4.com/en-GB/TVEpisode/hollyoaks-series-22-episode-175",
            elem.getFirstChildElement("ItemId", LAKEVIEW.getUri()).getValue());
        assertEquals("http://www.channel4.com/programmes/hollyoaks/episode-guide/series-22/episode-175.atom",
            elem.getFirstChildElement("PublicWebUri", LAKEVIEW.getUri()).getValue());
        assertEquals("https://xbox.channel4.com/pmlsd/hollyoaks/4od.atom?schema=2#555/555",
            elem.getFirstChildElement("ApplicationSpecificData", LAKEVIEW.getUri()).getValue());
        
    }
    
    @Test
    public void testUsesBrandGenreOnSeries() {
        Brand brand = createBrand();
        Series series = createSeries(brand);
        createEpisode("Title", 23, false, brand, series);
        brand.setGenres(ImmutableSet.of("http://www.channel4.com/programmes/categories/comedy"));
        Element seriesElem = feedCompiler.createSeriesElem(series, brand, new DateTime(), null);
        
        Elements genresElem = seriesElem.getChildElements("Genres", LAKEVIEW.getUri());
        assertEquals("Comedy", genresElem.get(0).getChildElements("Genre", LAKEVIEW.getUri()).get(0).getValue());
    }
    
    @Test
    public void testCreatesTotalNumberOfEpisodesElement() {
        
        Brand brand = createBrand();
        Series series = createSeries(brand);
        Episode episode1 = createEpisode("Episode 1", 1, false, brand, series);
        Episode episode2 = createEpisode("Episode 2", 2, false, brand, series);
        
        series.setChildRefs(ImmutableSet.of(episode1.childRef(), episode2.childRef()));
        
        LakeviewContentGroup contentGroup = new LakeviewContentGroup(brand, ImmutableList.of(series), 
                ImmutableList.of(episode1, episode2));
        
        Document doc = feedCompiler.compile(ImmutableList.of(contentGroup));
        Element seriesElem = doc.getRootElement()
                                .getChildElements("TVSeries", LAKEVIEW.getUri()).get(0);
        
        assertEquals(seriesElem.getChildElements("TotalNumberOfEpisodes", LAKEVIEW.getUri())
                               .get(0).getValue(), "2");
        
    }
    
    @Test
    public void testCreatesSortTitleElement() {
        Element episodeElem = createEpisodeElement("A Hard Day's Night", 1, false);
        assertEquals(episodeElem.getChildElements("SortTitle", LAKEVIEW.getUri()).get(0).getValue(), 
                     "Hard Day's Night, A");
    }
    
}
