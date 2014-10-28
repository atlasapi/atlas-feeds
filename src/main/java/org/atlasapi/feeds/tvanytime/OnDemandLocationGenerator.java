package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.entity.Item;

import tva.metadata._2010.OnDemandProgramType;

public interface OnDemandLocationGenerator {
    
    Iterable<OnDemandProgramType> generate(Item item);
}
