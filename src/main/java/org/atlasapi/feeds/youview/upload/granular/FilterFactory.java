package org.atlasapi.feeds.youview.upload.granular;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Identified;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;


public class FilterFactory {

    private static final String TERRESTRIAL_PROGRAMME_CRID_NS = "bbc:terrestrial_programme_crid:teleview";
    
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
                // programme crids aren't present on all broadcasts, and the only reason to
                // send a broadcast is for the linking between EPG items and on-demands, via
                // programme crids. Therefore we filter broadcasts without pcrids
                return hasPCrid(input.broadcast())
                         && (hasBeenUpdated(input.item(), updatedSince.get())
                            || hasBeenUpdated(input.version(), updatedSince.get())
                            || hasBeenUpdated(input.broadcast(), updatedSince.get()));
            }

        };
    }

    private static boolean hasPCrid(Broadcast broadcast) {
        return Iterables.tryFind(broadcast.getAliases(), new Predicate<Alias>() {
            @Override
            public boolean apply(Alias input) {
                return TERRESTRIAL_PROGRAMME_CRID_NS.equals(input.getNamespace());
            }
        }).isPresent();
    };
    
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
