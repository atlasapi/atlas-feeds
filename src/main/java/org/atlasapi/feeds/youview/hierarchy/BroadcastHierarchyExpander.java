package org.atlasapi.feeds.youview.hierarchy;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.services.BroadcastServiceMapping;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;


public class BroadcastHierarchyExpander {
    
    private final Logger log = LoggerFactory.getLogger(BroadcastHierarchyExpander.class);
    private final IdGenerator idGenerator;
    private final BroadcastServiceMapping serviceMapping;
    private final BbcServiceIdResolver serviceIdResolver;

    public BroadcastHierarchyExpander(IdGenerator idGenerator, BroadcastServiceMapping serviceMapping, 
            BbcServiceIdResolver serviceIdResolver) {
        this.idGenerator = checkNotNull(idGenerator);
        this.serviceMapping = checkNotNull(serviceMapping);
        this.serviceIdResolver = checkNotNull(serviceIdResolver);
    }

    public Map<String, ItemBroadcastHierarchy> expandHierarchy(Item item) {
        Iterable<ItemBroadcastHierarchy> itemBroadcastHierarchies = expandBroadcastHierarchiesFor(item);
        
        // can't use ImmutableMap.Builder as if generated ids are non-unique, map.put will throw 
        Map<String, ItemBroadcastHierarchy> broadcastHierarchies = Maps.newHashMap();
        for (ItemBroadcastHierarchy broadcastHierarchy : itemBroadcastHierarchies) {
            broadcastHierarchies.put(
                    idGenerator.generateBroadcastImi(broadcastHierarchy.youViewServiceId(), broadcastHierarchy.broadcast()), 
                    broadcastHierarchy
            );
        }
        return ImmutableMap.copyOf(broadcastHierarchies);
    }

    private Iterable<ItemBroadcastHierarchy> expandBroadcastHierarchiesFor(Item item) {
        return FluentIterable.from(item.getVersions())
                .transformAndConcat(expandBroadcastHierarchyFor(item));
    }

    private Function<Version, Iterable<ItemBroadcastHierarchy>> expandBroadcastHierarchyFor(final Item item) {
        return new Function<Version, Iterable<ItemBroadcastHierarchy>>() {
            @Override
            public Iterable<ItemBroadcastHierarchy> apply(Version input) {
                return expandBroadcastHierarchyFor(item, input);
            }
        };
    }
    
    private Iterable<ItemBroadcastHierarchy> expandBroadcastHierarchyFor(final Item item, final Version version) {
        return FluentIterable.from(version.getBroadcasts())
                .transformAndConcat(new Function<Broadcast, Iterable<ItemBroadcastHierarchy>>() {
                    @Override
                    public Iterable<ItemBroadcastHierarchy> apply(Broadcast input) {
                        return expandBroadcastHierarchyFor(item, version, input);
                    }
                }
        );
    }
    
    private Iterable<ItemBroadcastHierarchy> expandBroadcastHierarchyFor(final Item item, final Version version, final Broadcast broadcast) {
        Optional<Iterable<String>> youViewServiceIds 
            = serviceIdResolver.resolveSId(broadcast)
                               .transform(new Function<String, Iterable<String>>() {
                                                    @Override
                                                    public Iterable<String> apply(
                                                            String input) {
                                                        return serviceMapping.youviewServiceIdFor(input);
                                                    }
                                               }
                                          );
        
        if (!youViewServiceIds.isPresent() || Iterables.isEmpty(youViewServiceIds.get())) {
            log.warn("broadcast {} on {} has no mapped YouView service IDs", broadcast.getCanonicalUri(), broadcast.getBroadcastOn());
            return ImmutableList.of();
        }
        
        return Iterables.transform(youViewServiceIds.get(), new Function<String, ItemBroadcastHierarchy>() {
            @Override
            public ItemBroadcastHierarchy apply(String input) {
                return new ItemBroadcastHierarchy(item, version, broadcast, input);
            }
        });
    }
}
