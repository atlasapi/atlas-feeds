package org.atlasapi.feeds.radioplayer.outputting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class RadioPlayerDelegatingItemFilterTest {


	@Test
	public void testFilter() {
		RadioPlayerItemFilter filterFirst = new RadioPlayerItemFilter() {
			@Override
			public List<Item> filter(Iterable<Item> items, String service, DateTime day) {
				return ImmutableList.copyOf(Iterables.skip(items, 1));
			}
		};
		
		RadioPlayerItemFilter filterLast = new RadioPlayerItemFilter() {
			
			@Override
			public List<Item> filter(Iterable<Item> items, String service, DateTime day) {
				ArrayList<Item> listItems = Lists.newArrayList(items);
				return ImmutableList.copyOf(listItems.subList(0, listItems.size()-1));
			}
		};
		
		RadioPlayerItemFilter delegator = RadioPlayerDelegatingItemFilter.from(ImmutableList.of(filterFirst, filterLast));
		
		Item itemOne = new Item("one",":one",Publisher.BBC);
		Item itemTwo = new Item("two",":two",Publisher.BBC);
		Item itemThree = new Item("three",":three",Publisher.BBC);
		
		List<Item> filtered = delegator.filter(ImmutableList.of(itemOne,itemTwo,itemThree), null, null);
		
		assertThat(filtered, is(equalTo((List<Item>)ImmutableList.of(itemTwo))));
	}

}
