package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import tva.metadata._2010.DerivedFromType;
import tva.metadata._2010.ProgramInformationType;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;


public abstract class AbstractProgramInformationGenerator implements ProgramInformationGenerator {
    
    private final IdGenerator idGenerator;
    private final VersionHierarchyExpander hierarchyExpander;
    
    public AbstractProgramInformationGenerator(IdGenerator idGenerator, VersionHierarchyExpander hierarchyExpander) {
        this.idGenerator = checkNotNull(idGenerator);
        this.hierarchyExpander = checkNotNull(hierarchyExpander);
    }

    @Override
    public final Iterable<ProgramInformationType> generate(final Item item) {
        Map<String, ItemAndVersion> versionHierarchies = hierarchyExpander.expandHierarchy(item);
        
        return Iterables.transform(versionHierarchies.entrySet(), new Function<Entry<String, ItemAndVersion>, ProgramInformationType>() {
            @Override
            public ProgramInformationType apply(Entry<String, ItemAndVersion> input) {
                return generate(input.getKey(), input.getValue().item(), input.getValue().version());
            }
        });
    }

    public abstract ProgramInformationType generate(String versionCrid, Item item, Version version);
    
    public final DerivedFromType generateDerivedFromElem(Item item) {
        DerivedFromType derivedFrom = new DerivedFromType();
        derivedFrom.setCrid(idGenerator.generateContentCrid(item));
        return derivedFrom;
    }
}
