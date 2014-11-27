package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.CRIDRefType;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;


public abstract class AbstractBroadcastEventGenerator implements BroadcastEventGenerator {

    private final IdGenerator idGenerator;
    private final BroadcastHierarchyExpander hierarchyExpander;
    
    public AbstractBroadcastEventGenerator(IdGenerator idGenerator, 
            BroadcastHierarchyExpander hierarchyExpander) {
        this.idGenerator = checkNotNull(idGenerator);
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
    }

    @Override
    public final Iterable<BroadcastEventType> generate(Item item) {
        Map<String, ItemBroadcastHierarchy> broadcastHierarchy = hierarchyExpander.expandHierarchy(item);
        
        return Iterables.transform(broadcastHierarchy.entrySet(), new Function<Entry<String, ItemBroadcastHierarchy>, BroadcastEventType>() {
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
}
