package org.atlasapi.feeds.interlinking.outputting;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.atlasapi.feeds.interlinking.InterlinkBrand;
import org.atlasapi.feeds.interlinking.InterlinkFeed;
import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class InterlinkFeedOutputterTest {

	private final static InterlinkFeedOutputter outputter = new InterlinkFeedOutputter();
	
	@Test
	public void testSerialisationOfAFeed() throws Exception {
		
		InterlinkBrand brand = new InterlinkBrand("1").withTitle("Lark Rise to Candleford");
		
		InterlinkFeed feed = new InterlinkFeed("https://www.bbc.co.uk/interlinking/20100115")
			.withTitle("BBC Daily Change Feed")
			.withSubtitle("All metadata changes for on demand BBC content")
			.withUpdatedAt(new DateTime("2010-01-15T14:51:10Z"))
			.withAuthor(new InterlinkFeedAuthor("a partner", "a supplier"))
			.addBrand(brand);
		
		assertEquals(expectedFeed("brand.atom"), output(feed));
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
