package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;

import tva.metadata._2010.CRIDRefType;
import tva.metadata._2010.OnDemandProgramType;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;


public abstract class AbstractOnDemandLocationGenerator implements OnDemandLocationGenerator {

    private final IdGenerator idGenerator;
    
    public AbstractOnDemandLocationGenerator(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }
    
    @Override
    public Iterable<OnDemandProgramType> generate(final Item item) {
        Iterable<ItemOnDemandHierarchy> itemOndemandHierarchies = expandOnDemandHierarchyFor(item);
        
        Map<String, ItemOnDemandHierarchy> onDemandImis = Maps.newHashMap();
        for (ItemOnDemandHierarchy itemHierarchy : itemOndemandHierarchies) {
            onDemandImis.put(
                    idGenerator.generateOnDemandImi(
                        itemHierarchy.item(), 
                        itemHierarchy.version(),
                        itemHierarchy.encoding(),
                        itemHierarchy.location()
                    ), 
                    itemHierarchy
            );
        }
        
        return Iterables.transform(onDemandImis.entrySet(), new Function<Entry<String, ItemOnDemandHierarchy>, OnDemandProgramType>() {
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
