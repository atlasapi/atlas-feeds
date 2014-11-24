package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.nitro.BbcServiceIdResolver;
import org.atlasapi.feeds.youview.services.BroadcastServiceMapping;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.CRIDRefType;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;


public abstract class AbstractBroadcastEventGenerator implements BroadcastEventGenerator {

    private final Logger log = LoggerFactory.getLogger(AbstractBroadcastEventGenerator.class);
    
    private final IdGenerator idGenerator;
    private final BroadcastServiceMapping serviceMapping;
    private final BbcServiceIdResolver serviceIdResolver;
    
    public AbstractBroadcastEventGenerator(IdGenerator idGenerator, BroadcastServiceMapping serviceMapping, 
            BbcServiceIdResolver serviceIdResolver) {
        this.idGenerator = checkNotNull(idGenerator);
        this.serviceMapping = checkNotNull(serviceMapping);
        this.serviceIdResolver = checkNotNull(serviceIdResolver);
    }

    @Override
    public Iterable<BroadcastEventType> generate(Item item) {
        Iterable<ItemBroadcastHierarchy> itemBroadcastHierarchies = expandBroadcastHierarchiesFor(item);
        
        Map<String, ItemBroadcastHierarchy> broadcastImis = Maps.newHashMap();
        for (ItemBroadcastHierarchy itemHierarchy : itemBroadcastHierarchies) {
            broadcastImis.put(
                    idGenerator.generateBroadcastImi(itemHierarchy.youViewServiceId(), itemHierarchy.broadcast()), 
                    itemHierarchy
            );
        }
        
        return Iterables.transform(broadcastImis.entrySet(), new Function<Entry<String, ItemBroadcastHierarchy>, BroadcastEventType>() {
            @Override
            public BroadcastEventType apply(Entry<String, ItemBroadcastHierarchy> input) {
                ItemBroadcastHierarchy hierarchy = input.getValue();
                return generate(
                        input.getKey(), 
                        hierarchy.item(), 
                        hierarchy.version(),
                        hierarchy.broadcast(),
                        hierarchy.youViewServiceId()
                );
            }
        });
    }
    
    public abstract BroadcastEventType generate(String imi, Item item, Version version, Broadcast broadcast, String youviewServiceId);
    
    public CRIDRefType createProgram(Item item, Version version) {
        CRIDRefType program = new CRIDRefType();
        program.setCrid(idGenerator.generateVersionCrid(item, version));
        return program;
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
        Iterable<String> youViewServiceIds = serviceMapping.youviewServiceIdFor(serviceIdResolver.resolveSId(broadcast));
        if (Iterables.isEmpty(youViewServiceIds)) {
            log.warn("broadcast {} on {} has no mapped YouView service IDs", broadcast.getCanonicalUri(), broadcast.getBroadcastOn());
            return ImmutableList.of();
        }
        return Iterables.transform(youViewServiceIds, new Function<String, ItemBroadcastHierarchy>() {
            @Override
            public ItemBroadcastHierarchy apply(String input) {
                return new ItemBroadcastHierarchy(item, version, broadcast, input);
            }
        });
    }
}
