package org.atlasapi.feeds.radioplayer.outputting;

import java.util.List;

import org.atlasapi.media.entity.Item;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableList;

public class RadioPlayerDelegatingItemFilter implements RadioPlayerItemFilter {

	public static RadioPlayerDelegatingItemFilter from(Iterable<RadioPlayerItemFilter> filters) {
		return new RadioPlayerDelegatingItemFilter(ImmutableList.copyOf(filters));
	}
	
	private final List<RadioPlayerItemFilter> delegates;
	
	private RadioPlayerDelegatingItemFilter(List<RadioPlayerItemFilter> filters) {
		this.delegates = filters;
	}

	@Override
	public List<Item> filter(Iterable<Item> items, String service, DateTime day) {
		for (RadioPlayerItemFilter delegate : delegates) {
			items = delegate.filter(items, service, day);
		}
		return ImmutableList.copyOf(items);
	}

}
