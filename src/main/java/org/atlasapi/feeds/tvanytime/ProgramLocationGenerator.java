package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.entity.Item;

import tva.metadata._2010.ProgramLocationType;

public interface ProgramLocationGenerator {
    public ProgramLocationType generate(Item item);
}
