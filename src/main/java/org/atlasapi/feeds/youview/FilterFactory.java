package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Platform;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.joda.time.DateTime;

public class FilterFactory {

    private FilterFactory() {
    }
    
    public static Predicate<ItemAndVersion> versionFilter(final DateTime updatedSince) {
        return new Predicate<ItemAndVersion>() {
            @Override
            public boolean apply(ItemAndVersion input) {
                return input.version().getDuration() != null
                        && (hasBeenUpdated(input.item(), updatedSince)
                        || hasBeenUpdated(input.version(), updatedSince));
            }
        };
    }

    public static Predicate<ItemBroadcastHierarchy> broadcastFilter(final DateTime updatedSince) {
        return new Predicate<ItemBroadcastHierarchy>() {
            @Override
            public boolean apply(ItemBroadcastHierarchy input) {
                return (hasBeenUpdated(input.item(), updatedSince)
                        || hasBeenUpdated(input.version(), updatedSince)
                        || hasBeenUpdated(input.broadcast(), updatedSince));
            }

        };
    }

    public static Predicate<ItemOnDemandHierarchy> onDemandFilter(final DateTime updatedSince) {
        return Predicates.and(
                platformFilter(), 
                new Predicate<ItemOnDemandHierarchy>() {
                    @Override
                    public boolean apply(ItemOnDemandHierarchy input) {
                        return hasBeenUpdated(input.location(), updatedSince)
                                && !isExpired(input.location());
                    }
                }
        );
    }

    private static boolean isExpired(Location location) {
        Policy policy = location.getPolicy();

        if (policy == null) {
            return false;
        }

        DateTime end = policy.getAvailabilityEnd();

        return end != null && end.isBeforeNow();
    }

    private static Predicate<ItemOnDemandHierarchy> platformFilter() {
        return new Predicate<ItemOnDemandHierarchy>() {
            @Override
            public boolean apply(ItemOnDemandHierarchy input) {
                Policy policy = input.location().getPolicy();
                return policy != null && Platform.YOUVIEW_IPLAYER.equals(policy.getPlatform());
            }
        };
    }

    public static boolean hasBeenUpdated(Identified identified, DateTime updatedSince) {
        if(identified == null){
            return false;
        }
        if(identified.getLastUpdated() == null){
            return true;
        }
        return !identified.getLastUpdated().isBefore(updatedSince);
    }
}
