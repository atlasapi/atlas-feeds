package org.atlasapi.feeds.tvanytime;

import javax.xml.bind.JAXBElement;

import org.atlasapi.media.entity.Content;

import tva.metadata._2010.TVAMainType;

public interface TvAnytimeGenerator {

    JAXBElement<TVAMainType> generateTVAnytimeFrom(Iterable<Content> contents);
}
