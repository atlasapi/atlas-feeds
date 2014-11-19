package org.atlasapi.feeds.youview.unbox;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.media.entity.Item;

import tva.metadata._2010.BroadcastEventType;

public class UnboxBroadcastEventGenerator implements BroadcastEventGenerator {

    public UnboxBroadcastEventGenerator() {
    }
    
    @Override
    public Iterable<BroadcastEventType> generate(Item item) {
        throw new UnsupportedOperationException("Broadcast Events are not supported for the Amazon Unbox publisher");
    }
}
