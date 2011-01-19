package org.atlasapi.feeds.radioplayer.outputting;

import java.util.List;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public class RadioPlayerBroadcastFilter implements RadioPlayerItemFilter {

	private final Predicate<Version> HAS_BROADCASTS = new Predicate<Version>() {
		@Override
		public boolean apply(Version version) {
			return !version.getBroadcasts().isEmpty();
		}
	};

	@Override
	public List<Item> filter(Iterable<Item> items, final String service, DateTime day) {
		final Interval localInterval = day.toLocalDate().toInterval();
		
		final Predicate<Broadcast> validBroadcast = new Predicate<Broadcast>() {
			@Override
			public boolean apply(Broadcast broadcast) {
				return broadcast.getBroadcastOn().equals(service)
					&& localInterval.contains(broadcast.getTransmissionTime())
					&& localInterval.contains(broadcast.getTransmissionEndTime());
			}
		};
		
		final Function<Version, Version> broadcastFilter = new Function<Version, Version>() {
			@Override
			public Version apply(Version version) {
				version.setBroadcasts(ImmutableSet.copyOf(Iterables.filter(version.getBroadcasts(), validBroadcast)));
				return version;
			}
		};
		
		return ImmutableList.copyOf(Iterables.transform(items, new Function<Item, Item>() {
			@Override
			public Item apply(Item item) {
				item.setVersions(ImmutableSet.copyOf(Iterables.filter(Iterables.transform(item.getVersions(), broadcastFilter), HAS_BROADCASTS)));
				return item;
			}
		}));
	}

}
