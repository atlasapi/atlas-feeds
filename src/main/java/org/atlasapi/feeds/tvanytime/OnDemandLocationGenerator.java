package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.entity.Item;

import com.google.common.base.Optional;

import tva.metadata._2010.OnDemandProgramType;

public interface OnDemandLocationGenerator {
    
    Optional<OnDemandProgramType> generate(Item item);
}
