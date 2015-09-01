package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;

import tva.metadata._2010.CreditsListType;

public interface CreditsItemGenerator {

    CreditsListType generate(Content content);

}
