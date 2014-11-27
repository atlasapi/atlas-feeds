package org.atlasapi.feeds.youview.lovefilm;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.media.entity.Item;

import tva.metadata._2010.BroadcastEventType;

public class LoveFilmBroadcastEventGenerator implements BroadcastEventGenerator {

    public LoveFilmBroadcastEventGenerator() {
    }
    
    @Override
    public Iterable<BroadcastEventType> generate(Item item) {
        throw new UnsupportedOperationException("Broadcast Events are not supported for the LOVEFiLM publisher");
    }
}
