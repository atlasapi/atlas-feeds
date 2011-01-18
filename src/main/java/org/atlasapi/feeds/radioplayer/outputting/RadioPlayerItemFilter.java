package org.atlasapi.feeds.radioplayer.outputting;

import java.util.List;

import org.atlasapi.media.entity.Item;
import org.joda.time.DateTime;

public interface RadioPlayerItemFilter {

	List<Item> filter(Iterable<Item> items, String service, DateTime day);

}