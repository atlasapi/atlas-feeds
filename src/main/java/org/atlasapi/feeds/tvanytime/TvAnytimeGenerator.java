package org.atlasapi.feeds.tvanytime;

import javax.xml.bind.JAXBElement;

import org.atlasapi.media.entity.Content;

import tva.metadata._2010.TVAMainType;

public interface TvAnytimeGenerator {

    // TODO should this throw a checked exception?
    JAXBElement<TVAMainType> generateTVAnytimeFrom(Content content) throws TvaGenerationException;
}
