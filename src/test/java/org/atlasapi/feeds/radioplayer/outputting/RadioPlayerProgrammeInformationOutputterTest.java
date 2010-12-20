package org.atlasapi.feeds.radioplayer.outputting;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Countries;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class RadioPlayerProgrammeInformationOutputterTest {

	private static final DateTimeZone TIMEZONE = DateTimeZone.forOffsetHours(8);
	private static RadioPlayerXMLOutputter outputter = new RadioPlayerProgrammeInformationOutputter();
	
	public static Item buildItem(){
		Item testItem = new Episode("http://www.bbc.co.uk/programmes/b00f4d9c",
				"bbc:b00f4d9c", Publisher.BBC);
		testItem.setTitle("BBC Electric Proms: Saturday Night Fever");
		testItem.setDescription("Another chance to hear Robin Gibb perform the Bee Gees' classic disco album with the BBC Concert Orchestra. It was recorded" +
				" for the BBC Electric Proms back in October 2008, marking 30 years since Saturday Night Fever soundtrack topped the UK charts.");
		testItem.setGenres(ImmutableSet.of(
				"http://www.bbc.co.uk/programmes/genres/music",
				"http://ref.atlasapi.org/genres/atlas/music")
		);
		testItem.setImage("http://www.bbc.co.uk/iplayer/images/episode/b00v6bbc_640_360.jpg");
		
		Version version = new Version();
		
		Broadcast broadcast = new Broadcast("http://www.bbc.co.uk/services/radio2", new DateTime(2008,10,25,18,30,0,0, TIMEZONE), new DateTime(2008,10,25,20,0,0,0, TIMEZONE));
		version.addBroadcast(broadcast);
		
		Encoding encoding = new Encoding();
		Location location = new Location();
		location.setUri("http://www.bbc.co.uk/iplayer/episode/b00f4d9c");
		Policy policy = new Policy();
		policy.setAvailabilityEnd(new DateTime(2010, 8, 28, 23, 40, 19, 0, TIMEZONE));
		policy.setAvailabilityStart(new DateTime(2010, 9,  4, 23, 02, 00, 0, TIMEZONE));
		policy.addAvailableCountry(Countries.GB);
		location.setPolicy(policy);
		location.setTransportType(TransportType.LINK);
		encoding.addAvailableAt(location);
		version.addManifestedAs(encoding);
		
		testItem.addVersion(version);
		
		return testItem;
	}

	public static List<Item> buildItems() {
		Item testItem = buildItem();
		
		Series series = new Series("seriesUri", "seriesCurie");
		series.setTitle("This is the series title");
		((Episode)testItem).setSeries(series);
		
		Brand brand = new Brand("http://www.bbc.co.uk/programmes/b006m9mf", "bbc:b006m9mf", Publisher.BBC);
		brand.setTitle("Electric Proms");
		((Episode)testItem).setBrand(brand);
		
		return ImmutableList.of(testItem);
	}
	
	@Test
	public void testOutputtingAPIFeed() throws Exception {
		Item testItem = buildItem();
		
		Series series = new Series("seriesUri", "seriesCurie");
		series.setTitle("This is the series title");
		((Episode)testItem).setSeries(series);
		
		Brand brand = new Brand("http://www.bbc.co.uk/programmes/b006m9mf", "bbc:b006m9mf", Publisher.BBC);
		brand.setTitle("Electric Proms");
		((Episode)testItem).setBrand(brand);

		assertEquals(expectedFeed("basicPIFeedTest.xml"), output(buildItems()));
	}
	
	@Test
	public void testOutputtingAPIFeedWithSeriesAndNoBrand() throws Exception {
		Item testItem = buildItem();
		
		Series series = new Series("seriesUri", "seriesCurie");
		series.setTitle("Series Title");
		((Episode)testItem).setSeries(series);
		
		assertEquals(expectedFeed("seriesNoBrandPIFeedTest.xml"), output(ImmutableList.of(testItem)));
	}

	private static String output(List<Item> items) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		outputter.output(new DateTime(2010, 9, 6, 0, 0, 0, 0, DateTimeZone.UTC),
						new RadioPlayerService(502, "radio2").withDabServiceId("e1_ce15_c222_0"), items, out);
		return out.toString(Charsets.UTF_8.toString());
	}

	private String expectedFeed(String filename) throws IOException {
		return Resources.toString(
				Resources.getResource("org/atlasapi/feeds/radioplayer/"
						+ filename), Charsets.UTF_8);
	}

}
