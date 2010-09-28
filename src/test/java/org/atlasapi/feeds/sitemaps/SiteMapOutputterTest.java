package org.atlasapi.feeds.sitemaps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Countries;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

public class SiteMapOutputterTest extends TestCase {

	private final SiteMapOutputter outputter = new SiteMapOutputter();
	
	
	public void testOutputtingASitemap() throws Exception {
		
		Item glee = new Episode("http://www.channel4.com/programmes/grand-designs/1", "c4:grand-designs_1", Publisher.C4);
		glee.setThumbnail("granddesignsThumbnail");
		glee.setTitle("Grand Designs");
		glee.setDescription("Building builds");
		glee.setImage("http://www.channel4.com/assets/programmes/images/grand-designs/series-7/9867d8e4-1c2e-422c-a99d-96257bd0e4ae_625x352.jpg");
		Brand brand = new Brand("http://www.channel4.com/programmes/grand-designs", "c4:grand-designs", Publisher.C4);
		brand.setTitle("Grand Designs");
		((Episode)glee).setBrand(brand);
		Version version = new Version();
		Encoding encoding = new Encoding();
		Location location = new Location();
		location.setUri("http://www.channel4.com/programmes/grand-designs/#3121617");
		Policy policy = new Policy();
		policy.setAvailabilityEnd(new DateTime(2007, 7, 16, 19, 20, 30, 0, DateTimeZone.forOffsetHours(8)));
		policy.addAvailableCountry(Countries.GB);
		policy.addAvailableCountry(Countries.IT);
		location.setPolicy(policy);
		location.setTransportType(TransportType.LINK);
		encoding.addAvailableAt(location);
		version.addManifestedAs(encoding);
		glee.addVersion(version);
		
		assertThat(output(ImmutableList.of(glee)), is(expectedFeed("sitemap-a.xml")));
	}
	
	private String output(List<Item> items) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		outputter.output(items, out);
		return out.toString(Charsets.UTF_8.toString());
	}

	private String expectedFeed(String filename) throws IOException {
		return Resources.toString(Resources.getResource("org/atlasapi/feeds/sitemaps/" + filename), Charsets.UTF_8);
	}	
}
