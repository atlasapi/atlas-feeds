package org.atlasapi.feeds.radioplayer.outputting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerBroadcastFilterTest {
	
	private final RadioPlayerItemFilter filter = new RadioPlayerBroadcastFilter();

	@Test
	public void testFiltersBroadcastsFromWrongDay() {
		Item one = new Item("one", ":one", Publisher.BBC);
		
		Version versionOne = new Version();

		Broadcast justTooEarly = new Broadcast("service", new DateTime(2011, 1, 16, 22, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 17, 1, 0, 0, 0, DateTimeZones.UTC));
		Broadcast wayTooLate = new Broadcast("service", new DateTime(2011, 1, 18, 15, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 18, 16, 0, 0, 0, DateTimeZones.UTC));
		Broadcast toKeep = new Broadcast("service", new DateTime(2011, 1, 17, 10, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 17, 11, 0, 0, 0, DateTimeZones.UTC));
		Broadcast wrongService = new Broadcast("wrong-service", new DateTime(2011, 1, 17, 10, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 17, 11, 0, 0, 0, DateTimeZones.UTC));
		Broadcast justTooLate = new Broadcast("service", new DateTime(2011, 1, 17, 22, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 18, 1, 0, 0, 0, DateTimeZones.UTC));

		versionOne.setBroadcasts(ImmutableSet.of(justTooEarly, wayTooLate, toKeep, wrongService, justTooLate));
		
		one.addVersion(versionOne);
		
		List<Item> filtered = filter.filter(ImmutableSet.of(one), "service", new DateTime(2011,1,17, 0,0,0,0, DateTimeZones.UTC));
		
		assertThat(filtered.get(0).getVersions().iterator().next().getBroadcasts().size(), is(equalTo(1)));
		assertThat(filtered.get(0).getVersions().iterator().next().getBroadcasts().iterator().next(), is(equalTo(toKeep)));
	}

	@Test
	public void testRemovesVersionsWithNoValidBroadcasts() {
		Item one = new Item("one", ":one", Publisher.BBC);
		
		Version versionOne = new Version();
		Broadcast toKeep = new Broadcast("service", new DateTime(2011, 1, 17, 10, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 17, 11, 0, 0, 0, DateTimeZones.UTC));
		versionOne.addBroadcast(toKeep);
		
		Version versionTwo = new Version();
		Broadcast wayTooLate = new Broadcast("service", new DateTime(2011, 1, 18, 15, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 18, 16, 0, 0, 0, DateTimeZones.UTC));
		versionTwo.addBroadcast(wayTooLate);
		
		one.addVersion(versionOne);
		one.addVersion(versionTwo);
		
		List<Item> filtered = filter.filter(ImmutableSet.of(one), "service", new DateTime(2011,1,17, 0,0,0,0, DateTimeZones.UTC));
		
		assertThat(filtered.get(0).getVersions().size(), is(equalTo(1)));
		assertThat(filtered.get(0).getVersions().iterator().next(), is(equalTo(versionOne)));
		
	}
}
