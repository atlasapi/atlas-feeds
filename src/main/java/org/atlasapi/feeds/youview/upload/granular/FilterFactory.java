package org.atlasapi.feeds.youview.upload.granular;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Identified;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;


public class FilterFactory {

    private FilterFactory() {
    }
    
    public static Predicate<ItemAndVersion> versionFilter(final Optional<DateTime> updatedSince) {
        if (!updatedSince.isPresent()) {
            return Predicates.alwaysTrue();
        }
        return new Predicate<ItemAndVersion>() {
            @Override
            public boolean apply(ItemAndVersion input) {
                return hasBeenUpdated(input.item(), updatedSince.get())
                        || hasBeenUpdated(input.version(), updatedSince.get());
            }
        };
    }

    public static Predicate<ItemBroadcastHierarchy> broadcastFilter(final Optional<DateTime> updatedSince) {
        if (!updatedSince.isPresent()) {
            return Predicates.alwaysTrue();
        }
        return new Predicate<ItemBroadcastHierarchy>() {
            @Override
            public boolean apply(ItemBroadcastHierarchy input) {
                return hasBeenUpdated(input.item(), updatedSince.get())
                        || hasBeenUpdated(input.version(), updatedSince.get())
                        || hasBeenUpdated(input.broadcast(), updatedSince.get());
            }
        };
    }

    public static Predicate<ItemOnDemandHierarchy> onDemandFilter(final Optional<DateTime> updatedSince) {
        if (!updatedSince.isPresent()) {
            return Predicates.alwaysTrue();
        }
        return new Predicate<ItemOnDemandHierarchy>() {
            @Override
            public boolean apply(ItemOnDemandHierarchy input) {
                return hasBeenUpdated(input.item(), updatedSince.get())
                        || hasBeenUpdated(input.version(), updatedSince.get())
                        || hasBeenUpdated(input.encoding(), updatedSince.get())
                        || hasBeenUpdated(input.location(), updatedSince.get());
            }
        };
    }

    public static boolean hasBeenUpdated(Identified identified, DateTime updatedSince) {
        return identified.getLastUpdated().isBefore(updatedSince);
    }
}
