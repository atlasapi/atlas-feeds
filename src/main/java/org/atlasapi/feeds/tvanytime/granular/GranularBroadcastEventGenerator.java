package org.atlasapi.feeds.tvanytime.granular;

import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;

import tva.metadata._2010.BroadcastEventType;


public interface GranularBroadcastEventGenerator {

    BroadcastEventType generate(ItemBroadcastHierarchy broadcast, String broadcastImi);
}
