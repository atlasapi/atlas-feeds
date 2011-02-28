package org.atlasapi.feeds.radioplayer;

import static com.google.common.collect.Iterables.concat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.content.criteria.operator.Operators;
import org.atlasapi.feeds.radioplayer.outputting.NoItemsException;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerBroadcastItem;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerProgrammeInformationOutputter;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerXMLOutputter;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Channel;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.time.DateTimeZones;

public abstract class RadioPlayerFeedCompiler {
    
    private final RadioPlayerXMLOutputter outputter;
    protected final KnownTypeQueryExecutor executor;
    protected final ScheduleResolver scheduleResolver;

    public RadioPlayerFeedCompiler(RadioPlayerXMLOutputter outputter, KnownTypeQueryExecutor executor, ScheduleResolver scheduleResolver) {
        this.outputter = outputter;
        this.executor = executor;
        this.scheduleResolver = scheduleResolver;
    }
    
    private static Map<String, RadioPlayerFeedCompiler> compilerMap;
    
    public static void init(KnownTypeQueryExecutor queryExecutor, ScheduleResolver scheduleResolver) {
        compilerMap = ImmutableMap.of(
                "PI",new RadioPlayerProgrammeInformationFeedCompiler(queryExecutor, scheduleResolver),
                "OD",new RadioPlayerOnDemandFeedCompiler(queryExecutor, scheduleResolver));
    }
    
    public static RadioPlayerFeedCompiler valueOf(String type) {
        return compilerMap.get(type);
    }
	
    private static class RadioPlayerProgrammeInformationFeedCompiler extends RadioPlayerFeedCompiler {
        public RadioPlayerProgrammeInformationFeedCompiler(KnownTypeQueryExecutor executor, ScheduleResolver scheduleResolver) {
            super(new RadioPlayerProgrammeInformationOutputter(), executor, scheduleResolver);
        }

        @Override
        public List<Item> queryFor(LocalDate day, String serviceUri) {
            DateTime date = day.toDateTimeAtStartOfDay(DateTimeZones.UTC);
            Channel channel = Channel.fromUri(serviceUri).requireValue();
            Schedule schedule = scheduleResolver.schedule(date.minusMillis(1), date.plusDays(1), ImmutableSet.of(channel), ImmutableSet.of(Publisher.BBC));
            return Iterables.getOnlyElement(schedule.scheduleChannels()).items();
        }
    }
    
    private static class RadioPlayerOnDemandFeedCompiler extends RadioPlayerFeedCompiler {
        public RadioPlayerOnDemandFeedCompiler(KnownTypeQueryExecutor executor, ScheduleResolver scheduleResolver) {
            super(null, executor, scheduleResolver);
        }

        @Override
        public List<Item> queryFor(LocalDate broadcastOn, String serviceUri) {
            Iterable<AtomicQuery> queryAtoms = ImmutableSet.of((AtomicQuery)
                    Attributes.BROADCAST_ON.createQuery(Operators.EQUALS, ImmutableList.of(serviceUri)),
                    Attributes.LOCATION_AVAILABLE.createQuery(Operators.EQUALS, ImmutableList.of(Boolean.TRUE))
            );
            return ImmutableList.copyOf(Iterables.filter(executor.discover(new ContentQuery(queryAtoms)), Item.class));
        }
    }

	public abstract List<Item> queryFor(LocalDate date, String serviceUri);
	
    public RadioPlayerXMLOutputter getOutputter() {
        if (outputter == null) {
            throw new UnsupportedOperationException(this.toString() + " feeds are not currently supported");
        }
        return outputter;
    }

    public void compileFeedFor(LocalDate day, RadioPlayerService service, OutputStream out) throws IOException {
        if (outputter != null) {
            String serviceUri = service.getServiceUri();
            List<Item> items = queryFor(day, serviceUri);
            if (items.isEmpty()) {
                throw new NoItemsException(day, service);
            }
            outputter.output(day, service, sort(transform(items, serviceUri, day)), out);
        }
    }

    private List<RadioPlayerBroadcastItem> sort(Iterable<RadioPlayerBroadcastItem> broadcastItems) {
        return Ordering.natural().immutableSortedCopy(broadcastItems);
    }

    private List<RadioPlayerBroadcastItem> transform(List<Item> items, String serviceUri, LocalDate day) {
        return ImmutableList.copyOf(concat(Iterables.transform(items, new Function<Item, Iterable<RadioPlayerBroadcastItem>>() {
            @Override
            public Iterable<RadioPlayerBroadcastItem> apply(Item item) {
                ArrayList<RadioPlayerBroadcastItem> broadcastItems = Lists.newArrayList();
                for (Version version : item.getVersions()) {
                    for (Broadcast broadcast : version.getBroadcasts()) {
                        broadcastItems.add(new RadioPlayerBroadcastItem(item, version, broadcast));
                    }
                }
                return broadcastItems;
            }
        })));
    }
}
