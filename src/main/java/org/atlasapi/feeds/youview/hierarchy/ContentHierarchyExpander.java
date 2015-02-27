package org.atlasapi.feeds.youview.hierarchy;

import java.util.Map;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;


// TODO consider renaming?
public interface ContentHierarchyExpander {

    String contentCridFor(Content content);
    Map<String, ItemAndVersion> versionHierarchiesFor(Item item);
    Map<String, ItemBroadcastHierarchy> broadcastHierarchiesFor(Item item);
    Map<String, ItemOnDemandHierarchy> onDemandHierarchiesFor(Item item);
}
