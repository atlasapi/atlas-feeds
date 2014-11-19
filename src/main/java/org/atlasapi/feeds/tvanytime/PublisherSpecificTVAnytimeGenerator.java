package org.atlasapi.feeds.tvanytime;

import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import tva.metadata._2010.TVAMainType;

import com.google.common.collect.ImmutableMap;


public class PublisherSpecificTVAnytimeGenerator implements TvAnytimeGenerator {
    
    private final Map<Publisher, TvAnytimeGenerator> generators;

    public PublisherSpecificTVAnytimeGenerator(Map<Publisher, TvAnytimeGenerator> generators) {
        this.generators = ImmutableMap.copyOf(generators);
    }

    @Override
    public JAXBElement<TVAMainType> generateTVAnytimeFrom(Content content) {
        Publisher publisher = content.getPublisher();
        TvAnytimeGenerator delegate = generators.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate.generateTVAnytimeFrom(content);
    }

}
