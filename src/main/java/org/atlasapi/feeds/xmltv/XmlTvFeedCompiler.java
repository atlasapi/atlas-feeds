package org.atlasapi.feeds.xmltv;

import static com.google.common.collect.Iterables.concat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.content.Container;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.LookupRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentCategory;
import org.atlasapi.persistence.content.KnownTypeContentResolver;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.metabroadcast.common.time.DateTimeZones;

public class XmlTvFeedCompiler {

    private static final LocalTime SCHEDULE_START = new LocalTime(06,00,00);
    private final ScheduleResolver scheduleResolver;
    private final Publisher publisher;
    private final KnownTypeContentResolver contentResolver;
    private final XmlTvFeedOutputter outputter;

    public XmlTvFeedCompiler(ScheduleResolver scheduleResolver, KnownTypeContentResolver contentResolver, Publisher publisher) {
        this.scheduleResolver = scheduleResolver;
        this.contentResolver = contentResolver;
        this.publisher = publisher;
        this.outputter = new XmlTvFeedOutputter();
    }
    
    public void compileChannelFeed(Range<LocalDate> days, Channel channel, OutputStream writeTo) throws IOException {
        outputter.output(sort(transform(getItemsFromSchedule(days, channel))), writeTo);
    }
    
    private List<XmlTvBroadcastItem> sort(List<XmlTvBroadcastItem> list) {
        return Ordering.natural().immutableSortedCopy(list);
    }
    
    private List<XmlTvBroadcastItem> transform(List<Item> items) {
        final Map<String, Identified> containers = containersFor(items);
        final Map<String, Identified> series = seriesFor(items);
        return ImmutableList.copyOf(concat(Iterables.transform(items, new Function<Item, Iterable<XmlTvBroadcastItem>>() {
            @Override
            public Iterable<XmlTvBroadcastItem> apply(Item item) {
                ArrayList<XmlTvBroadcastItem> broadcastItems = Lists.newArrayList();
                for (Version version : item.nativeVersions()) {
                    for (Broadcast broadcast : version.getBroadcasts()) {
                        XmlTvBroadcastItem broadcastItem = new XmlTvBroadcastItem(item, version, broadcast);
                        if(item.getContainer() != null && containers.containsKey(item.getContainer().getUri())) {
                            broadcastItem.withContainer((Container)containers.get(item.getContainer().getUri()));
                        }
                        if(item instanceof Episode && ((Episode)item).getSeriesRef() != null) {
                            broadcastItem.withSeries((Series)series.get(((Episode)item).getSeriesRef().getUri()));
                        }
                        broadcastItems.add(broadcastItem);
                    }
                }
                return broadcastItems;
            }
        })));
    }

    private List<Item> getItemsFromSchedule(Range<LocalDate> days, Channel channel) {
        if(channel.equals(XmlTvChannelLookup.IGNORED)) {
            return ImmutableList.of();
        }
        DateTime from = days.lowerEndpoint().toDateTime(SCHEDULE_START, DateTimeZones.UTC);
        DateTime to = days.upperEndpoint().toDateTime(SCHEDULE_START, DateTimeZones.UTC);
        Schedule schedule = scheduleResolver.schedule(from, to, ImmutableList.of(channel), ImmutableSet.of(publisher), Optional.<ApplicationConfiguration>absent());
        List<Item> items = Iterables.getOnlyElement(schedule.channelSchedules()).items();
        return items;
    }
    
    private Map<String, Identified> seriesFor(List<Item> items) {
        Iterable<LookupRef> containerLookups = Iterables.filter(Iterables.transform(Iterables.filter(items, Episode.class), new Function<Episode, LookupRef>() {

            @Override
            public LookupRef apply(Episode input) {
                if(input.getSeriesRef() != null) {
                    return new LookupRef(input.getSeriesRef().getUri(), input.getSeriesRef().getId(), input.getPublisher(), ContentCategory.PROGRAMME_GROUP);
                }
                return null;
            }
            
        }),Predicates.notNull());
        
        if(Iterables.isEmpty(containerLookups)) {
            return ImmutableMap.of();
        }
        
        return contentResolver.findByLookupRefs(ImmutableSet.copyOf(containerLookups)).asResolvedMap();
    }
    
    private Map<String, Identified> containersFor(List<Item> items) {
        Iterable<LookupRef> containerLookups = Iterables.filter(Iterables.transform(items, new Function<Item, LookupRef>() {

            @Override
            public LookupRef apply(Item input) {
                if(input.getContainer() != null) {
                    return new LookupRef(input.getContainer().getUri(), input.getContainer().getId(), input.getPublisher(), ContentCategory.CONTAINER);
                }
                return null;
            }
            
        }),Predicates.notNull());
        
        if(Iterables.isEmpty(containerLookups)) {
            return ImmutableMap.of();
        }
        
        return contentResolver.findByLookupRefs(ImmutableSet.copyOf(containerLookups)).asResolvedMap();
    }
}
