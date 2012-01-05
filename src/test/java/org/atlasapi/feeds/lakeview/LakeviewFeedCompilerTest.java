package org.atlasapi.feeds.lakeview;

import static org.junit.Assert.*;

import org.junit.Test;

public class LakeviewFeedCompilerTest {

	@Test
	public void testBrandAtomUri() {
		LakeviewFeedCompiler feedCompiler = new LakeviewFeedCompiler(null);
		assertEquals("https://xbox.channel4.com/pmlsd/educating-essex/4od.atom", 
				feedCompiler.brandAtomUri("http://www.channel4.com/programmes/educating-essex"));
	}
	
	@Test
	public void testSeriesAtomUri() {
		LakeviewFeedCompiler feedCompiler = new LakeviewFeedCompiler(null);
		assertEquals("https://xbox.channel4.com/pmlsd/educating-essex/4od.atom#series-1", 
				feedCompiler.seriesAtomUri("http://www.channel4.com/programmes/educating-essex/episode-guide/series-1"));
	}

	@Test
	public void testEpisodeAtomUri() {
		LakeviewFeedCompiler feedCompiler = new LakeviewFeedCompiler(null);
		assertEquals("https://xbox.channel4.com/pmlsd/educating-essex/4od.atom#12345", 
				feedCompiler.episodeAtomUri("http://www.channel4.com/programmes/educating-essex/episode-guide/series-1/episode-1", "12345"));
	
	}
}
