package org.atlasapi.feeds.youview.unbox;

import org.atlasapi.feeds.tvanytime.granular.GranularBroadcastEventGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;

import tva.metadata._2010.BroadcastEventType;

public class UnboxBroadcastEventGenerator implements GranularBroadcastEventGenerator {

    public UnboxBroadcastEventGenerator() { }

    @Override
    public BroadcastEventType generate(ItemBroadcastHierarchy broadcast, String broadcastImi) {
        throw new UnsupportedOperationException("Broadcast Events are not supported for the Amazon Unbox publisher");
    }
}
