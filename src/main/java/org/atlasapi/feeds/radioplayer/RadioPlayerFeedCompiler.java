package org.atlasapi.feeds.radioplayer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.LookupRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentCategory;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.KnownTypeContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.base.MorePredicates;
import com.metabroadcast.common.time.DateTimeZones;

public abstract class RadioPlayerFeedCompiler {
    
    private final RadioPlayerXMLOutputter outputter;
    protected final KnownTypeContentResolver knownTypeContentResolver;

    public RadioPlayerFeedCompiler(RadioPlayerXMLOutputter outputter, KnownTypeContentResolver contentResolver) {
        this.outputter = checkNotNull(outputter);
        this.knownTypeContentResolver = checkNotNull(contentResolver);
    }
    
    private static Map<FileType, RadioPlayerFeedCompiler> compilerMap;
    
    // not ideal - this leads to identical OD compilers for each publisher 
    public static void init(ScheduleResolver scheduleResolver, KnownTypeContentResolver knownTypeContentResolver, ContentResolver contentResolver, ChannelResolver channelResolver, Publisher publisher, RadioPlayerGenreElementCreator genreElementCreator) {
        compilerMap = createCompilerMapForPublisher(publisher, scheduleResolver, knownTypeContentResolver, contentResolver, channelResolver, genreElementCreator);
    }
    
    
    
    private static Map<FileType, RadioPlayerFeedCompiler> createCompilerMapForPublisher(Publisher publisher, 
            ScheduleResolver scheduleResolver, KnownTypeContentResolver knownTypeContentResolver, 
            ContentResolver contentResolver, ChannelResolver channelResolver, RadioPlayerGenreElementCreator genreElementCreator) {
        return ImmutableMap.<FileType, RadioPlayerFeedCompiler>of(
                PI, new RadioPlayerProgrammeInformationFeedCompiler(scheduleResolver, knownTypeContentResolver, channelResolver, publisher, genreElementCreator),
                OD, new RadioPlayerOnDemandFeedCompiler(knownTypeContentResolver, contentResolver, genreElementCreator)
            );
    }



    public static RadioPlayerFeedCompiler valueOf(FileType type) {
        checkState(compilerMap != null, "Compiler map not initialised");
        RadioPlayerFeedCompiler compiler = compilerMap.get(type);
        checkArgument(compiler != null, "No compiler for type " + type);
        return compiler;
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
    }
    
	public abstract List<Item> queryFor(RadioPlayerFeedSpec spec);
	
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

    private List<RadioPlayerBroadcastItem> transform(List<Item> items, String serviceUri) {
        final Map<String, Identified> containers = containersFor(items);
        return ImmutableList.copyOf(concat(Iterables.transform(items, new Function<Item, Iterable<RadioPlayerBroadcastItem>>() {
            @Override
            public Iterable<RadioPlayerBroadcastItem> apply(Item item) {
                ArrayList<RadioPlayerBroadcastItem> broadcastItems = Lists.newArrayList();
                for (Version version : item.nativeVersions()) {
                    for (Broadcast broadcast : version.getBroadcasts()) {
                        RadioPlayerBroadcastItem broadcastItem = new RadioPlayerBroadcastItem(item, version, broadcast);
                        if(item.getContainer() != null && containers.containsKey(item.getContainer().getUri())) {
                            broadcastItem.withContainer((Container)containers.get(item.getContainer().getUri()));
                        }
                        broadcastItems.add(broadcastItem);
                    }
                }
                return broadcastItems;
            }
        })));
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
        
        return knownTypeContentResolver.findByLookupRefs(ImmutableSet.copyOf(containerLookups)).asResolvedMap();
    }
}
