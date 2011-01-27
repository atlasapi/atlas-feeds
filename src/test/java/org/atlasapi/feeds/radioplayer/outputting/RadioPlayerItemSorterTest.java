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

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerItemSorterTest {

	private final RadioPlayerItemSorter sorter = new RadioPlayerItemSorter();
	
	@Test
	public void testSortsItems() {
		Item itemOne = addBroadcastTo(new Item("uriOne", "curie", Publisher.BBC), new DateTime(2011, 1, 17, 10, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 17, 11, 0, 0, 0, DateTimeZones.UTC));
		Item itemTwo = addBroadcastTo(new Item("uriTwo", "curie", Publisher.BBC), new DateTime(2011, 1, 17, 15, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 17, 18, 0, 0, 0, DateTimeZones.UTC));
		
		List<RadioPlayerBroadcastItem> sorted = sorter.sortAndTransform(ImmutableList.of(itemTwo, itemOne), null, null);
        assertThat(sorted.size(), is(equalTo(2)));
		assertThat(sorted.get(0).getItem(), is(equalTo(itemOne)));
		assertThat(sorted.get(1).getItem(), is(equalTo(itemTwo)));
	}
	
	@Test //There's potential for an Item to be broadcast twice on the same day on the same service
	public void testAddsTwiceBroadcastItemTwice() {
		Item itemOne = addBroadcastTo(new Item("uriOne", "curie", Publisher.BBC), new DateTime(2011, 1, 17, 10, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 17, 11, 0, 0, 0, DateTimeZones.UTC));
		addBroadcastTo(itemOne, new DateTime(2011, 1, 17, 19, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 17, 20, 0, 0, 0, DateTimeZones.UTC));
		Item itemTwo = addBroadcastTo(new Item("uriTwo", "curie", Publisher.BBC), new DateTime(2011, 1, 17, 15, 0, 0, 0, DateTimeZones.UTC), new DateTime(2011, 1, 17, 18, 0, 0, 0, DateTimeZones.UTC));
		
        List<RadioPlayerBroadcastItem> sorted = sorter.sortAndTransform(ImmutableList.of(itemTwo, itemOne), null, null);
        assertThat(sorted.size(), is(equalTo(3)));
        assertThat(sorted.get(0).getItem(), is(equalTo(itemOne)));
        assertThat(sorted.get(1).getItem(), is(equalTo(itemTwo)));
        assertThat(sorted.get(2).getItem(), is(equalTo(itemOne)));
	}

	private Item addBroadcastTo(Item item, DateTime transmissionStart, DateTime transmissionEnd) {
		Version version = new Version();
		
		Broadcast broadcast = new Broadcast("service", transmissionStart, transmissionEnd);
		
		version.addBroadcast(broadcast);
		item.addVersion(version);
		
		return item;
	}

}
