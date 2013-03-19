package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.entity.Item;

import tva.metadata._2010.ProgramInformationType;

public interface ProgramInformationGenerator {
    ProgramInformationType generate(Item item);
}
