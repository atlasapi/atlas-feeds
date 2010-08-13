package org.atlasapi.feeds.interlinking.outputting;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.atlasapi.feeds.interlinking.InterlinkBrand;
import org.atlasapi.feeds.interlinking.InterlinkBroadcast;
import org.atlasapi.feeds.interlinking.InterlinkEpisode;
import org.atlasapi.feeds.interlinking.InterlinkFeed;
import org.atlasapi.feeds.interlinking.InterlinkOnDemand;
import org.atlasapi.feeds.interlinking.InterlinkSeries;
import org.atlasapi.feeds.interlinking.InterlinkBase.Operation;
import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.atlasapi.feeds.interlinking.validation.InterlinkOutputValidator;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.metabroadcast.common.time.DateTimeZones;

public class InterlinkFeedOutputterTest {

	private final static InterlinkFeedOutputter outputter = new InterlinkFeedOutputter();
	private final DateTime lastUpdated = new DateTime("2010-04-27T09:49:40.803Z", DateTimeZones.UTC);
	
	@Test
	public void testSerialisationOfAFeed() throws Exception {
		
		InterlinkBrand brand = new InterlinkBrand("1", Operation.STORE)
			.withTitle("Lark Rise to Candleford")
			.withDescription("Adaption of Flora Thompson's memoir of her Oxfordshire childhood")
			.withLastUpdated(lastUpdated)
			.withThumbnail("thumbnail");
		
		// add an episode directly to the brand
		InterlinkEpisode episodeWithoutASeries = new InterlinkEpisode("notInASeries", Operation.STORE, 2, "link").withTitle("Episode not in a series").withLastUpdated(lastUpdated);
		
		episodeWithoutASeries.addBroadcast(new InterlinkBroadcast("broadcastNotInASeries", Operation.STORE));
		episodeWithoutASeries.addOnDemand(new InterlinkOnDemand("odNotInASeries", Operation.STORE, lastUpdated, lastUpdated, new Duration(1000)));
		
		brand.addEpisodeWithoutASeries(episodeWithoutASeries);
		
		InterlinkSeries series = new InterlinkSeries("series2", Operation.STORE, 2)
			.withTitle("Lark Rise to Candleford Series 2")
			.withSummary("Adaption of Flora Thompson's");
		
		brand.addSeries(series);
		
		InterlinkOnDemand onDemand = new InterlinkOnDemand("ondemand5", Operation.STORE, lastUpdated, lastUpdated, new Duration(1000));
		
		InterlinkBroadcast broadcast = new InterlinkBroadcast("broadcast4", Operation.STORE)
			.withBroadcastStart(new DateTime("2010-01-10T21:00:00Z"))
			.withDuration(Duration.standardMinutes(45)).withLastUpdated(lastUpdated);
		
		InterlinkEpisode episode = new InterlinkEpisode("episode3", Operation.STORE, 3, "link")
			.withTitle("Lark Rise to Candleford Episode 3")
			.addBroadcast(broadcast)
			.addOnDemand(onDemand)
			.withLastUpdated(lastUpdated);
		
		series.addEpisode(episode);
		
		InterlinkFeed feed = new InterlinkFeed("https://www.bbc.co.uk/interlinking/20100115")
			.withTitle("BBC Daily Change Feed")
			.withSubtitle("All metadata changes for on demand BBC content")
			.withUpdatedAt(lastUpdated)
			.withAuthor(new InterlinkFeedAuthor("a partner", "a supplier"))
			.addBrand(brand);
		
		String generated = output(feed);
		assertEquals(expectedFeed("feed.atom"), generated);
		
		// for now just print out errors since the feed is not finished and will not validate
		new InterlinkOutputValidator().validatesAgainstSchema(generated, System.out);
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
