package org.atlasapi.feeds.tvanytime;

import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.tvanytime.granular.GranularTvAnytimeGenerator;
import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import tva.metadata._2010.TVAMainType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;


public class PublisherSpecificGranularTVAnytimeGenerator implements GranularTvAnytimeGenerator {
    
    private final Map<Publisher, GranularTvAnytimeGenerator> generators;

    public PublisherSpecificGranularTVAnytimeGenerator(Map<Publisher, GranularTvAnytimeGenerator> generators) {
        this.generators = ImmutableMap.copyOf(generators);
    }

    @Override
    public JAXBElement<TVAMainType> generateContentTVAFrom(Content content)
            throws TvaGenerationException {
        GranularTvAnytimeGenerator delegate = fetchDelegateOrThrow(content);
        return delegate.generateContentTVAFrom(content);
    }

    @Override
    public JAXBElement<TVAMainType> generateVersionTVAFrom(ItemAndVersion version, String versionCrid)
            throws TvaGenerationException {
        GranularTvAnytimeGenerator delegate = fetchDelegateOrThrow(version.item());
        return delegate.generateVersionTVAFrom(version, versionCrid);
    }

    @Override
    public JAXBElement<TVAMainType> generateBroadcastTVAFrom(ItemBroadcastHierarchy broadcast,
            String broadcastImi) throws TvaGenerationException {
        GranularTvAnytimeGenerator delegate = fetchDelegateOrThrow(broadcast.item());
        return delegate.generateBroadcastTVAFrom(broadcast, broadcastImi);
    }

    @Override
    public JAXBElement<TVAMainType> generateOnDemandTVAFrom(ItemOnDemandHierarchy onDemand,
            String onDemandImi) throws TvaGenerationException {
        GranularTvAnytimeGenerator delegate = fetchDelegateOrThrow(onDemand.item());
        return delegate.generateOnDemandTVAFrom(onDemand, onDemandImi);
    }

    @Override
    public JAXBElement<TVAMainType> generateContentTVAFrom(Content content,
            Map<String, ItemAndVersion> versions, Map<String, ItemBroadcastHierarchy> broadcasts,
            Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException {
        GranularTvAnytimeGenerator delegate = fetchDelegateOrThrow(content);
        return delegate.generateContentTVAFrom(content, versions, broadcasts, onDemands);
    }

    @Override
    public JAXBElement<TVAMainType> generateVersionTVAFrom(Map<String, ItemAndVersion> versions)
            throws TvaGenerationException {
        ItemAndVersion hierarchy = Iterables.getFirst(versions.values(), null);
        if (hierarchy == null) {
            throw new InvalidPublisherException("no content to obtain publisher");
        }
        GranularTvAnytimeGenerator delegate = fetchDelegateOrThrow(hierarchy.item());
        return delegate.generateVersionTVAFrom(versions);
    }

    @Override
    public JAXBElement<TVAMainType> generateBroadcastTVAFrom(
            Map<String, ItemBroadcastHierarchy> broadcasts) throws TvaGenerationException {
        ItemBroadcastHierarchy hierarchy = Iterables.getFirst(broadcasts.values(), null);
        if (hierarchy == null) {
            throw new InvalidPublisherException("no content to obtain publisher");
        }
        GranularTvAnytimeGenerator delegate = fetchDelegateOrThrow(hierarchy.item());
        return delegate.generateBroadcastTVAFrom(broadcasts);
    }

    @Override
    public JAXBElement<TVAMainType> generateOnDemandTVAFrom(
            Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException {
        ItemOnDemandHierarchy hierarchy = Iterables.getFirst(onDemands.values(), null);
        if (hierarchy == null) {
            throw new InvalidPublisherException("no content to obtain publisher");
        }
        GranularTvAnytimeGenerator delegate = fetchDelegateOrThrow(hierarchy.item());
        return delegate.generateOnDemandTVAFrom(onDemands);
    }

    private GranularTvAnytimeGenerator fetchDelegateOrThrow(Content content) {
        Publisher publisher = content.getPublisher();
        GranularTvAnytimeGenerator delegate = generators.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate;
    }
}
