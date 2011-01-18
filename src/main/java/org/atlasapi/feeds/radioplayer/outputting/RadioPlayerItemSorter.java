package org.atlasapi.feeds.radioplayer.outputting;

import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class RadioPlayerItemSorter implements RadioPlayerItemFilter {
	
	private final Comparator<? super Broadcast> broadcastComparator = new Comparator<Broadcast>() {
		@Override
		public int compare(Broadcast b1, Broadcast b2) {
			return b1.getTransmissionTime().compareTo(b2.getTransmissionTime());
		}};

	public List<Item> filter(Iterable<Item> items, String service, DateTime day) {
		
		SortedMap<Broadcast,Item> broadcastItems = Maps.newTreeMap(broadcastComparator);
		
		for (Item item : items) {
			for (Version version : item.getVersions()) {
				for (Broadcast broadcast : version.getBroadcasts()) {
					broadcastItems.put(broadcast, item);
				}
			}
		}
		
		return ImmutableList.copyOf(broadcastItems.values());
	}
	
}
