package org.atlasapi.feeds.radioplayer;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.base.MorePredicates;
import com.metabroadcast.common.stream.MoreCollectors;
import com.metabroadcast.common.time.DateTimeZones;
import org.atlasapi.feeds.radioplayer.outputting.NoItemsException;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerBroadcastItem;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerGenreElementCreator;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerProgrammeInformationOutputter;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerUpdatedClipOutputter;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerXMLOutputter;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.LookupRef;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentCategory;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.KnownTypeContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

public abstract class RadioPlayerFeedCompiler {
    
    private final RadioPlayerXMLOutputter outputter;
    protected final KnownTypeContentResolver knownTypeContentResolver;

    public RadioPlayerFeedCompiler(RadioPlayerXMLOutputter outputter, KnownTypeContentResolver contentResolver) {
        this.outputter = outputter;
        this.knownTypeContentResolver = contentResolver;
    }
    
    private static Map<Publisher, Map<FileType, RadioPlayerFeedCompiler>> compilerMap;
    
    // not ideal - this leads to identical OD compilers for each publisher 
    public static void init(ScheduleResolver scheduleResolver, KnownTypeContentResolver knownTypeContentResolver, ContentResolver contentResolver, ChannelResolver channelResolver, Iterable<Publisher> publishers, Map<Publisher,RadioPlayerGenreElementCreator> genreElementCreators) {
        ImmutableMap.Builder<Publisher, Map<FileType, RadioPlayerFeedCompiler>> map = ImmutableMap.builder();
        for (Publisher publisher : publishers) {
            RadioPlayerGenreElementCreator genreElementCreator = checkNotNull(genreElementCreators.get(publisher));
            map.put(publisher, createCompilerMapForPublisher(publisher, scheduleResolver, knownTypeContentResolver, contentResolver, channelResolver, genreElementCreator ));
        }
        compilerMap = map.build();
    }
    
    
    
    private static Map<FileType, RadioPlayerFeedCompiler> createCompilerMapForPublisher(Publisher publisher, 
            ScheduleResolver scheduleResolver, KnownTypeContentResolver knownTypeContentResolver, 
            ContentResolver contentResolver, ChannelResolver channelResolver, RadioPlayerGenreElementCreator genreElementCreator) {
        return ImmutableMap.<FileType, RadioPlayerFeedCompiler>of(
                PI, new RadioPlayerProgrammeInformationFeedCompiler(scheduleResolver, knownTypeContentResolver, channelResolver, publisher, genreElementCreator),
                OD, new RadioPlayerOnDemandFeedCompiler(knownTypeContentResolver, contentResolver, genreElementCreator)
            );
    }



    public static RadioPlayerFeedCompiler valueOf(Publisher publisher, FileType type) {
        checkState(compilerMap != null, "Compiler map not initialised");
        return checkNotNull(compilerMap.get(publisher).get(type), "No compiler for publisher " + publisher + " and type " + type);
    }
	
    private static class RadioPlayerProgrammeInformationFeedCompiler extends RadioPlayerFeedCompiler {
        private final ScheduleResolver scheduleResolver;
        private final ChannelResolver channelResolver;
        private final Publisher publisher;

        public RadioPlayerProgrammeInformationFeedCompiler(ScheduleResolver scheduleResolver, KnownTypeContentResolver knownTypeContentResolver, ChannelResolver channelResolver, Publisher publisher, RadioPlayerGenreElementCreator genreElementCreator) {
            super(new RadioPlayerProgrammeInformationOutputter(genreElementCreator), knownTypeContentResolver);
            this.scheduleResolver = scheduleResolver;
            this.channelResolver = channelResolver;
            this.publisher = publisher;
        }

        @Override
        public List<Item> queryFor(RadioPlayerFeedSpec spec) {
            checkArgument(spec instanceof RadioPlayerPiFeedSpec);
            DateTime date = ((RadioPlayerPiFeedSpec)spec).getDay().toDateTimeAtStartOfDay(DateTimeZones.UTC);
            Channel channel = channelResolver.fromUri(spec.getService().getServiceUri()).requireValue();
            Schedule schedule = scheduleResolver.unmergedSchedule(date.minusMillis(1), date.plusDays(1), ImmutableSet.of(channel), ImmutableSet.of(publisher));
            return Iterables.getOnlyElement(schedule.scheduleChannels()).items();
        }

        @Override
        public List<RadioPlayerBroadcastItem> transform(List<Item> items, String serviceUri) {
            final Map<String, Identified> containers = containersFor(items);
            return items.stream()
                    .map(item -> {
                        //we only expect to find one due to how schedule resolver works
                        Broadcast broadcast = findFirstBroadcast(item);
                        Version version = findAvailableVersionOrFirstVersion(item);
                        RadioPlayerBroadcastItem broadcastItem = new RadioPlayerBroadcastItem(
                                item,
                                version,
                                broadcast
                        );
                        if (item.getContainer() != null && containers.containsKey(item.getContainer().getUri())) {
                            broadcastItem.withContainer((Container) containers.get(item.getContainer().getUri()));
                        }
                        return broadcastItem;
                    })
                    .collect(MoreCollectors.toImmutableList());
        }

        private Broadcast findFirstBroadcast(Item item) {
            for (Version version : item.nativeVersions()) {
                for (Broadcast broadcast : version.getBroadcasts()) {
                    return broadcast;
                }
            }
            throw new IllegalArgumentException("Found no broadcast on " + item);
        }

        private Version findAvailableVersionOrFirstVersion(Item item) {
            for (Version version : item.nativeVersions()) {
                for (Encoding encoding : version.getManifestedAs()) {
                    for (Location location : encoding.getAvailableAt()) {
                        if (isAvailable(location)) {
                            return version;
                        }
                    }
                }
            }
            return item.getVersions().iterator().next();
        }

        private boolean isAvailable(Location location) {
            Policy policy = location.getPolicy();
            return (policy.getAvailabilityStart() == null || policy.getAvailabilityStart().isBeforeNow())
                    && (policy.getAvailabilityEnd() == null || policy.getAvailabilityEnd().isAfterNow());
        }
    }
    
    private static class RadioPlayerOnDemandFeedCompiler extends RadioPlayerFeedCompiler {
        
        private final ContentResolver contentResolver;

        public RadioPlayerOnDemandFeedCompiler(KnownTypeContentResolver knownTypeContentResolver, ContentResolver contentResolver, RadioPlayerGenreElementCreator genreElementCreator) {
            super(new RadioPlayerUpdatedClipOutputter(genreElementCreator), knownTypeContentResolver);
            this.contentResolver = contentResolver;
        }
        
        @Override
        public List<Item> queryFor(RadioPlayerFeedSpec spec) {
            checkArgument(spec instanceof RadioPlayerOdFeedSpec);
            RadioPlayerOdFeedSpec odSpec = (RadioPlayerOdFeedSpec)spec;
            ResolvedContent resolvedContent = contentResolver.findByCanonicalUris(odSpec.getUris());
            return ImmutableList.copyOf(filter(filter(resolvedContent.getAllResolvedResults(), Item.class), 
                    MorePredicates.transformingPredicate(Item.TO_CLIPS, MorePredicates.anyPredicate(RadioPlayerUpdatedClipOutputter.availableAndUpdatedSince(odSpec.getSince()))))); 
        }

        @Override
        public List<RadioPlayerBroadcastItem> transform(List<Item> items, String serviceUri) {
            final Map<String, Identified> containers = containersFor(items);
            return items.stream()
                    .map(item -> {
                        ArrayList<RadioPlayerBroadcastItem> broadcastItems = Lists.newArrayList();
                        for (Version version : item.nativeVersions()) {
                            RadioPlayerBroadcastItem broadcastItem = new RadioPlayerBroadcastItem(
                                    item,
                                    version,
                                    null //broadcast information is not used for OD
                            );
                            if (item.getContainer() != null && containers.containsKey(item.getContainer().getUri())) {
                                broadcastItem.withContainer((Container) containers.get(item.getContainer().getUri()));
                            }
                            broadcastItems.add(broadcastItem);
                        }
                        return broadcastItems;
                    })
                    .flatMap(Collection::stream)
                    .collect(MoreCollectors.toImmutableList());
        }
    }
    
	public abstract List<Item> queryFor(RadioPlayerFeedSpec spec);

    public abstract List<RadioPlayerBroadcastItem> transform(List<Item> items, String serviceUri);

    public RadioPlayerXMLOutputter getOutputter() {
        if (outputter == null) {
            throw new UnsupportedOperationException(this.toString() + " feeds are not currently supported");
        }
        return outputter;
    }
    
    public void compileFeedFor(RadioPlayerFeedSpec spec, OutputStream out) throws IOException {
        if (outputter != null) {
            List<Item> items = queryFor(spec);
            if (items.isEmpty()) {
                throw new NoItemsException(spec);
            }
            outputter.output(spec, sort(transform(items, spec.getService().getServiceUri())), out);
        }
    }

    private List<RadioPlayerBroadcastItem> sort(Iterable<RadioPlayerBroadcastItem> broadcastItems) {
        return Ordering.natural().immutableSortedCopy(broadcastItems);
    }

    protected Map<String, Identified> containersFor(List<Item> items) {
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
        
        return knownTypeContentResolver.findByLookupRefs(ImmutableSet.copyOf(containerLookups)).asResolvedMap();
    }
}
