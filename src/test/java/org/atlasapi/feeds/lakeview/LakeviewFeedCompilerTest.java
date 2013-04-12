package org.atlasapi.feeds.lakeview;

import static org.junit.Assert.*;

import nu.xom.Element;
import nu.xom.Elements;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class LakeviewFeedCompilerTest {

    private static final XMLNamespace LAKEVIEW = new XMLNamespace("", "http://schemas.microsoft.com/Lakeview/2011/06/13/ingestion");

	@Test
	public void testBrandAtomUri() {
		LakeviewFeedCompiler feedCompiler = new LakeviewFeedCompiler(null, false);
		assertEquals("https://xbox.channel4.com/pmlsd/educating-essex/4od.atom", 
				feedCompiler.brandAtomUri("http://www.channel4.com/programmes/educating-essex"));
	}
	
	@Test
	public void testSeriesAtomUri() {
		LakeviewFeedCompiler feedCompiler = new LakeviewFeedCompiler(null, false);
		assertEquals("https://xbox.channel4.com/pmlsd/educating-essex/4od.atom#series-1", 
				feedCompiler.seriesAtomUri("http://www.channel4.com/programmes/educating-essex/episode-guide/series-1"));
	}

	@Test
	public void testEpisodeAtomUri() {
		LakeviewFeedCompiler feedCompiler = new LakeviewFeedCompiler(null, false);
		assertEquals("https://xbox.channel4.com/pmlsd/educating-essex/4od.atom#12345", 
				feedCompiler.episodeAtomUri("http://www.channel4.com/programmes/educating-essex/episode-guide/series-1/episode-1", "12345"));
	
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
	    String seriesTitle = "Lord of the Wrongs";
	    Element element = getSeriesElement(seriesTitle, 12, true);
	    Elements titles = element.getChildElements("Title", LAKEVIEW.getUri());

	    assertEquals("Should have exactly one title", 1, titles.size());
	    assertEquals("Title should be generic", "Series 12", titles.get(0).getValue());
	}

	private Element getSeriesElement(String seriesTitle, int seriesNumber, boolean genericTitleEnabled) {
        LakeviewFeedCompiler feedCompiler = new LakeviewFeedCompiler(null, genericTitleEnabled);
        Series series = new Series("seriesUri", "seriesCurie", null);
        Brand parent = new Brand("brandUri", "brandCurie", null);
        series.setTitle(seriesTitle);
        series.withSeriesNumber(seriesNumber);
        
        return feedCompiler.createSeriesElem(series, parent, new DateTime(), null);
	}
	
	@Test
	public void testNonGenericEpisodeTitleGenerated() {
	    String episodeTitle="The One That Was Funny";
	    
        Element element = getEpisodeElement(episodeTitle, 3012, false);
        Elements titles = element.getChildElements("Title", LAKEVIEW.getUri());
        
        assertEquals("Should have exactly one title", 1, titles.size());
        assertEquals("Title should be as set", episodeTitle, titles.get(0).getValue());
	}

	@Test
	public void testGenericEpisodeTitleGenerated() {
	    String episodeTitle="The One That Was Funny";

	    Element element = getEpisodeElement(episodeTitle, 3012, true);
	    Elements titles = element.getChildElements("Title", LAKEVIEW.getUri());

	    assertEquals("Should have exactly one title", 1, titles.size());
	    assertEquals("Title should be generic", "Episode 3012", titles.get(0).getValue());
	}

    private Element getEpisodeElement(String episodeTitle, int episodeNumber, boolean genericTitleEnabled) {
        LakeviewFeedCompiler feedCompiler = new LakeviewFeedCompiler(null, genericTitleEnabled);
	    Episode episode = new Episode("episodeUri", "episodeCurie", null);
	    Brand container = new Brand("brandUri", "brandCurie", null);
	    episode.setTitle(episodeTitle);
	    episode.setEpisodeNumber(episodeNumber);
	    
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
	    episode.setContainer(container);
	    
	    return feedCompiler.createEpisodeElem(episode, container, new DateTime(), null);
    }
}
