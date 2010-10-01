package org.atlasapi.feeds.radioplayer.outputting;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerServiceIdentifier;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Countries;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
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
	private RadioPlayerXMLOutputter outputter = new RadioPlayerProgrammeInformationOutputter();

	@Test
	public void testOutputtingASitemap() throws Exception {

		Item testItem = new Item("http://www.bbc.co.uk/programmes/b00f4d9c",
				"bbc:b00f4d9c", Publisher.BBC);
		testItem.setTitle("BBC Electric Proms: Saturday Night Fever");
		testItem.setDescription("Another chance to hear Robin Gibb perform the Bee Gees' classic disco album with the BBC Concert Orchestra. It was recorded" +
				" for the BBC Electric Proms back in October 2008, marking 30 years since Saturday Night Fever soundtrack topped the UK charts. Robin performs" +
				" alongside guest artists including Sam Sparro, Sharleen Spiteri, Ronan Keating, Stephen Gateley and Gabriella Cilmi at London's Roundhouse. " +
				"The show is directed by Oscar-winning composer Anne Dudley, accompanied by the BBC Concert Orchestra, and includes Stayin' Alive, Night Fever," +
				" Jive Talkin' and More Than A Woman.");
		testItem.setGenres(ImmutableSet.of(
				"http://www.bbc.co.uk/programmes/genres/music",
				"http://ref.atlasapi.org/genres/atlas/music"));
		
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

		assertEquals(expectedFeed("20100906_e1_ce15_c221_0_PI.xml"), output(ImmutableList.of(testItem)));
	}

	private String output(List<Item> items) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		outputter.output(new DateTime(2010, 9, 6, 0, 0, 0, 0),
						new RadioPlayerServiceIdentifier(502, "http://www.bbc.co.uk/services/radio2","e1_ce15_c222_0"), items, out);
		return out.toString(Charsets.UTF_8.toString());
	}

	private String expectedFeed(String filename) throws IOException {
		return Resources.toString(
				Resources.getResource("org/atlasapi/feeds/radioplayer/"
						+ filename), Charsets.UTF_8);
	}

}
