package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.collect.Iterators.filter;

import java.util.Iterator;
import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerUpdatedClipOutputter;
import org.atlasapi.media.content.Broadcast;
import org.atlasapi.media.content.Content;
import org.atlasapi.media.content.Item;
import org.atlasapi.media.content.Publisher;
import org.atlasapi.media.content.Version;
import org.atlasapi.media.content.util.ContentCategory;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.listing.ContentListingCriteria;
import org.atlasapi.persistence.content.listing.LastUpdatedContentFinder;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.metabroadcast.common.base.MoreOrderings;
import com.metabroadcast.common.base.MorePredicates;

public class RadioPlayerOdUriResolver {
    
    private final Ordering<Broadcast> byTransmissionTime = MoreOrderings.<Broadcast, DateTime>transformingOrdering(Broadcast.TO_TRANSMISSION_TIME, Ordering.<DateTime>natural());
    private final ContentLister contentLister;
    private final LastUpdatedContentFinder lastUpdatedContentFinder;

    public RadioPlayerOdUriResolver(ContentLister contentLister, LastUpdatedContentFinder lastUpdatedContentFinder) {
        this.contentLister = contentLister;
        this.lastUpdatedContentFinder = lastUpdatedContentFinder;
    }
    
    public SetMultimap<RadioPlayerService, String> getServiceToUrisMapForSnapshot() {
        
        return getServiceToUrisMapForContent(contentLister.listContent(new ContentListingCriteria.Builder().forPublisher(Publisher.BBC).forContent(ImmutableList.copyOf(ContentCategory.ITEMS)).build()), Optional.<DateTime>absent());
    }
    
    public SetMultimap<RadioPlayerService, String> getServiceToUrisMapSince(DateTime since) {
        
        return getServiceToUrisMapForContent(lastUpdatedContentFinder.updatedSince(Publisher.BBC, since), Optional.of(since));
    }

    private SetMultimap<RadioPlayerService, String> getServiceToUrisMapForContent(Iterator<Content> content, Optional<DateTime> since) {
        
        HashMultimap<RadioPlayerService, String> serviceToUris = HashMultimap.create();
        
        Iterator<Item> items = filter(filter(content, Item.class), MorePredicates.transformingPredicate(Item.TO_CLIPS, MorePredicates.anyPredicate(RadioPlayerUpdatedClipOutputter.availableAndUpdatedSince(since))));
        
        while (items.hasNext()) {
            Item item = items.next();
            
            Set<Broadcast> allBroadcasts = Sets.newHashSet();
            for (Version version : item.getVersions()) {
                allBroadcasts.addAll(version.getBroadcasts());
            }
            
            if (!allBroadcasts.isEmpty()) {
                Broadcast firstBroadcast = byTransmissionTime.min(allBroadcasts);
                
                RadioPlayerService service = RadioPlayerServices.serviceUriToService.get(firstBroadcast.getBroadcastOn());
                if (service != null) {
                    serviceToUris.put(service, item.getCanonicalUri());
                }
            }
        }
        
        return serviceToUris;
    }
}
