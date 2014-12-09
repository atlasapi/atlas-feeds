package org.atlasapi.feeds.youview.hierarchy;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;


public final class VersionHierarchyExpander {

    private final IdGenerator idGenerator;

    public VersionHierarchyExpander(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }
    
    public Map<String, ItemAndVersion> expandHierarchy(final Item item) {
        Iterable<ItemAndVersion> itemsWithVersions = Iterables.transform(item.getVersions(), new Function<Version, ItemAndVersion>() {
            @Override
            public ItemAndVersion apply(Version input) {
                return new ItemAndVersion(item, input);
            }
        });
        
        // can't use ImmutableMap.Builder as if generated ids are non-unique, map.put will throw 
        Map<String, ItemAndVersion> versionHierarchies = Maps.newHashMap();
        for (ItemAndVersion itemAndVersion : itemsWithVersions) {
            versionHierarchies.put(idGenerator.generateVersionCrid(itemAndVersion.item(), itemAndVersion.version()), itemAndVersion);
        }
        return ImmutableMap.copyOf(versionHierarchies);
    }
}
