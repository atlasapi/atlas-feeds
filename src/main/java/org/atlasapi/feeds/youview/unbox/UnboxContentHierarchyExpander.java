package org.atlasapi.feeds.youview.unbox;

import java.util.Map;

import org.atlasapi.feeds.youview.hierarchy.BroadcastHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.hierarchy.OnDemandHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.VersionHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;

import static com.google.common.base.Preconditions.checkNotNull;

public class UnboxContentHierarchyExpander implements ContentHierarchyExpander {

    private final VersionHierarchyExpander versionExpander;
    private final BroadcastHierarchyExpander broadcastExpander;
    private final OnDemandHierarchyExpander onDemandExpander;
    private final IdGenerator idGenerator;

    public UnboxContentHierarchyExpander(VersionHierarchyExpander versionExpander,
            BroadcastHierarchyExpander broadcastExpander, OnDemandHierarchyExpander onDemandExpander,
            IdGenerator idGenerator) {
        this.versionExpander = checkNotNull(versionExpander);
        this.broadcastExpander = checkNotNull(broadcastExpander);
        this.onDemandExpander = checkNotNull(onDemandExpander);
        this.idGenerator = checkNotNull(idGenerator);
    }

    @Override
    public String contentCridFor(Content content) {
        return idGenerator.generateContentCrid(content);
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
