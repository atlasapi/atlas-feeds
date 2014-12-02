package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.media.entity.Item;


public class NitroContentHierarchyExpander implements ContentHierarchyExpander {
    
    private final VersionHierarchyExpander versionExpander;
    private final BroadcastHierarchyExpander broadcastExpander;
    private final OnDemandHierarchyExpander onDemandExpander;
    
    public NitroContentHierarchyExpander(VersionHierarchyExpander versionExpander,
            BroadcastHierarchyExpander broadcastExpander, OnDemandHierarchyExpander onDemandExpander) {
        this.versionExpander = checkNotNull(versionExpander);
        this.broadcastExpander = checkNotNull(broadcastExpander);
        this.onDemandExpander = checkNotNull(onDemandExpander);
    }

    @Override
    public Map<String, ItemAndVersion> versionHierarchiesFor(Item item) {
        return versionExpander.expandHierarchy(item);
    }

    @Override
    public Map<String, ItemBroadcastHierarchy> broadcastHierarchiesFor(Item item) {
        return broadcastExpander.expandHierarchy(item);
    }

    @Override
    public Map<String, ItemOnDemandHierarchy> onDemandHierarchiesFor(Item item) {
        return onDemandExpander.expandHierarchy(item);
    }

}
