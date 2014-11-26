package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;

import tva.metadata._2010.CRIDRefType;
import tva.metadata._2010.OnDemandProgramType;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;


public abstract class AbstractOnDemandLocationGenerator implements OnDemandLocationGenerator {

    private final IdGenerator idGenerator;
    private final OnDemandHierarchyExpander hierarchyExpander;
    
    public AbstractOnDemandLocationGenerator(IdGenerator idGenerator, OnDemandHierarchyExpander hierarchyExpander) {
        this.idGenerator = checkNotNull(idGenerator);
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
    }
    
    @Override
    public Iterable<OnDemandProgramType> generate(Item item) {
        Map<String, ItemOnDemandHierarchy> onDemandHierarchies = hierarchyExpander.expandHierarchy(item);
        
        return Iterables.transform(onDemandHierarchies.entrySet(), new Function<Entry<String, ItemOnDemandHierarchy>, OnDemandProgramType>() {
            @Override
            public OnDemandProgramType apply(Entry<String, ItemOnDemandHierarchy> input) {
                ItemOnDemandHierarchy hierarchy = input.getValue();
                return generate(
                        input.getKey(), 
                        hierarchy.item(), 
                        hierarchy.version(),
                        hierarchy.encoding(),
                        hierarchy.location()
                );
            }
        });
    }
    
    public abstract OnDemandProgramType generate(String imi, Item item, Version version, Encoding encoding, Location location);
    
    public CRIDRefType generateProgram(Item item, Version version) {
        CRIDRefType program = new CRIDRefType();
        program.setCrid(idGenerator.generateVersionCrid(item, version));
        return program;
    }
}
