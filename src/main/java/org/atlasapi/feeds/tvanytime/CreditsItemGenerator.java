package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.entity.Item;

import tva.metadata._2010.CreditsListType;

public interface CreditsItemGenerator {

    CreditsListType generate(Item item);

}
