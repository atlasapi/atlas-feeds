package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import tva.metadata._2010.DerivedFromType;
import tva.metadata._2010.ProgramInformationType;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;


public abstract class AbstractProgramInformationGenerator implements ProgramInformationGenerator {
    
    private final IdGenerator idGenerator;
    
    public AbstractProgramInformationGenerator(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }

    @Override
    public final Iterable<ProgramInformationType> generate(final Item item) {
        Iterable<ItemAndVersion> itemsWithVersions = Iterables.transform(item.getVersions(), new Function<Version, ItemAndVersion>() {
            @Override
            public ItemAndVersion apply(Version input) {
                return new ItemAndVersion(item, input);
            }
        });
        
        // TODO will this cause issues if fields on version that differ are used to generate on-demands?
        // may pick the incorrect version here
        Map<String, ItemAndVersion> versionCrids = Maps.newHashMap();
        for (ItemAndVersion itemAndVersion : itemsWithVersions) {
            versionCrids.put(idGenerator.generateVersionCrid(itemAndVersion.item(), itemAndVersion.version()), itemAndVersion);
        }
        
        return Iterables.transform(versionCrids.entrySet(), new Function<Entry<String, ItemAndVersion>, ProgramInformationType>() {
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
