package org.atlasapi.feeds.tvanytime;

import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;

import tva.metadata._2010.BroadcastEventType;


public interface BroadcastEventGenerator {

    BroadcastEventType generate(ItemBroadcastHierarchy broadcast, String broadcastImi);
}
