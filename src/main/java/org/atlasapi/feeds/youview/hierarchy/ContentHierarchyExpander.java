package org.atlasapi.feeds.youview.hierarchy;

import java.util.Map;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;


public interface ContentHierarchyExpander {
    IdGenerator getIdGenerator();

    String contentCridFor(Content content);
    Map<String, ItemAndVersion> versionHierarchiesFor(Item item);
    Map<String, ItemBroadcastHierarchy> broadcastHierarchiesFor(Item item);
    Map<String, ItemOnDemandHierarchy> onDemandHierarchiesFor(Item item);
}
