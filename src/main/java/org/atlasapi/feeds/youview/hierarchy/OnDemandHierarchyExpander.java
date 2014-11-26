package org.atlasapi.feeds.youview.hierarchy;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;


public class OnDemandHierarchyExpander {

    private final IdGenerator idGenerator;
    
    public OnDemandHierarchyExpander(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }

    public Map<String, ItemOnDemandHierarchy> expandHierarchy(Item item) {
        Iterable<ItemOnDemandHierarchy> itemOndemandHierarchies = expandOnDemandHierarchyFor(item);
        
        // can't use ImmutableMap.Builder as if generated ids are non-unique, map.put will throw
        Map<String,ItemOnDemandHierarchy> onDemandHierarchies = Maps.newHashMap();
        for (ItemOnDemandHierarchy itemHierarchy : itemOndemandHierarchies) {
            onDemandHierarchies.put(
                    idGenerator.generateOnDemandImi(
                        itemHierarchy.item(), 
                        itemHierarchy.version(),
                        itemHierarchy.encoding(),
                        itemHierarchy.location()
                    ), 
                    itemHierarchy
            );
        }
        return ImmutableMap.copyOf(onDemandHierarchies);
    }

    private Iterable<ItemOnDemandHierarchy> expandOnDemandHierarchyFor(Item item) {
        return FluentIterable.from(item.getVersions())
                .transformAndConcat(toOnDemandHierarchy(item));
    }

    private Function<Version, Iterable<ItemOnDemandHierarchy>> toOnDemandHierarchy(final Item item) {
        return new Function<Version, Iterable<ItemOnDemandHierarchy>>() {
            @Override
            public Iterable<ItemOnDemandHierarchy> apply(Version input) {
                return toOnDemandHierarchy(item, input, input.getManifestedAs());
            }
        };
    }

    private Iterable<ItemOnDemandHierarchy> toOnDemandHierarchy(final Item item, final Version version, 
            Iterable<Encoding> encodings) {
        return FluentIterable.from(encodings)
                .transformAndConcat(toOnDemandHierarchy(item, version));
    }

    private Function<Encoding, Iterable<ItemOnDemandHierarchy>> toOnDemandHierarchy(final Item item, final Version version) {
        return new Function<Encoding, Iterable<ItemOnDemandHierarchy>>() {
            @Override
            public Iterable<ItemOnDemandHierarchy> apply(Encoding input) {
                return toOnDemandHierarchy(item, version, input, input.getAvailableAt());
            }
        };
    }

    private Iterable<ItemOnDemandHierarchy> toOnDemandHierarchy(final Item item, final Version version, 
            final Encoding encoding, Iterable<Location> locations) {
        return Iterables.transform(locations, new Function<Location, ItemOnDemandHierarchy>() {
            @Override
            public ItemOnDemandHierarchy apply(Location input) {
                return new ItemOnDemandHierarchy(item, version, encoding, input);
            }
        });
    }
}
