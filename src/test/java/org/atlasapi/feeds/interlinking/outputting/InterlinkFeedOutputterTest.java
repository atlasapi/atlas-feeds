package org.atlasapi.feeds.interlinking.outputting;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.atlasapi.feeds.interlinking.InterlinkBrand;
import org.atlasapi.feeds.interlinking.InterlinkBroadcast;
import org.atlasapi.feeds.interlinking.InterlinkEpisode;
import org.atlasapi.feeds.interlinking.InterlinkFeed;
import org.atlasapi.feeds.interlinking.InterlinkSeries;
import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class InterlinkFeedOutputterTest {

	private final static InterlinkFeedOutputter outputter = new InterlinkFeedOutputter();
	
	@Test
	public void testSerialisationOfAFeed() throws Exception {
		
		InterlinkBrand brand = new InterlinkBrand("1")
			.withTitle("Lark Rise to Candleford");

		InterlinkSeries series = new InterlinkSeries("series2", 2)
			.withTitle("Lark Rise to Candleford Series 2");
		
		brand.addSeries(series);
		
		InterlinkBroadcast broadcast = new InterlinkBroadcast("broadcast4")
			.withBroadcastStart(new DateTime("2010-01-10T21:00:00Z"))
			.withDuration(Duration.standardMinutes(45));
		
		InterlinkEpisode episode = new InterlinkEpisode("episode3", 3)
			.withTitle("Lark Rise to Candleford Episode 3")
			.addBroadcast(broadcast);
		series.addEpisode(episode);
		
		InterlinkFeed feed = new InterlinkFeed("https://www.bbc.co.uk/interlinking/20100115")
			.withTitle("BBC Daily Change Feed")
			.withSubtitle("All metadata changes for on demand BBC content")
			.withUpdatedAt(new DateTime("2010-01-15T14:51:10Z"))
			.withAuthor(new InterlinkFeedAuthor("a partner", "a supplier"))
			.addBrand(brand);
		
		assertEquals(expectedFeed("feed.atom"), output(feed));
	}

	private String output(InterlinkFeed feed) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		outputter.output(feed, out);
		return out.toString(Charsets.UTF_8.toString());
	}

	private String expectedFeed(String filename) throws IOException {
		return Resources.toString(Resources.getResource("org/atlasapi/feeds/interlinking/outputting/" + filename), Charsets.UTF_8);
	}	
}
