package org.atlasapi.feeds.tvanytime;

import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import tva.metadata._2010.TVAMainType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;


public class PublisherSpecificTVAnytimeGenerator implements TvAnytimeGenerator {
    
    private final Map<Publisher, TvAnytimeGenerator> generators;

    public PublisherSpecificTVAnytimeGenerator(Map<Publisher, TvAnytimeGenerator> generators) {
        this.generators = ImmutableMap.copyOf(generators);
    }

    @Override
    public JAXBElement<TVAMainType> generateChannelTVAFrom(Channel channel)
            throws TvaGenerationException {
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(channel);
        return delegate.generateChannelTVAFrom(channel);
    }

    @Override
    public JAXBElement<TVAMainType> generateChannelTVAFrom(Channel channel, Channel parentChannel)
            throws TvaGenerationException {
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(channel);
        return delegate.generateChannelTVAFrom(channel, parentChannel);
    }

    @Override
    public JAXBElement<TVAMainType> generateContentTVAFrom(Content content)
            throws TvaGenerationException {
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(content);
        return delegate.generateContentTVAFrom(content);
    }

    @Override
    public JAXBElement<TVAMainType> generateVersionTVAFrom(ItemAndVersion version, String versionCrid)
            throws TvaGenerationException {
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(version.item());
        return delegate.generateVersionTVAFrom(version, versionCrid);
    }

    @Override
    public JAXBElement<TVAMainType> generateBroadcastTVAFrom(ItemBroadcastHierarchy broadcast,
            String broadcastImi) throws TvaGenerationException {
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(broadcast.item());
        return delegate.generateBroadcastTVAFrom(broadcast, broadcastImi);
    }

    @Override
    public JAXBElement<TVAMainType> generateOnDemandTVAFrom(ItemOnDemandHierarchy onDemand,
            String onDemandImi) throws TvaGenerationException {
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(onDemand.item());
        return delegate.generateOnDemandTVAFrom(onDemand, onDemandImi);
    }

    @Override
    public JAXBElement<TVAMainType> generateContentTVAFrom(Content content,
            Map<String, ItemAndVersion> versions, Map<String, ItemBroadcastHierarchy> broadcasts,
            Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException {
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(content);
        return delegate.generateContentTVAFrom(content, versions, broadcasts, onDemands);
    }

    @Override
    public JAXBElement<TVAMainType> generateVersionTVAFrom(Map<String, ItemAndVersion> versions)
            throws TvaGenerationException {
        ItemAndVersion hierarchy = Iterables.getFirst(versions.values(), null);
        if (hierarchy == null) {
            throw new InvalidPublisherException("no content to obtain publisher");
        }
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(hierarchy.item());
        return delegate.generateVersionTVAFrom(versions);
    }

    @Override
    public JAXBElement<TVAMainType> generateBroadcastTVAFrom(
            Map<String, ItemBroadcastHierarchy> broadcasts) throws TvaGenerationException {
        ItemBroadcastHierarchy hierarchy = Iterables.getFirst(broadcasts.values(), null);
        if (hierarchy == null) {
            throw new InvalidPublisherException("no content to obtain publisher");
        }
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(hierarchy.item());
        return delegate.generateBroadcastTVAFrom(broadcasts);
    }

    @Override
    public JAXBElement<TVAMainType> generateOnDemandTVAFrom(
            Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException {
        ItemOnDemandHierarchy hierarchy = Iterables.getFirst(onDemands.values(), null);
        if (hierarchy == null) {
            throw new InvalidPublisherException("no content to obtain publisher");
        }
        TvAnytimeGenerator delegate = fetchDelegateOrThrow(hierarchy.item());
        return delegate.generateOnDemandTVAFrom(onDemands);
    }

    private TvAnytimeGenerator fetchDelegateOrThrow(Content content) {
        Publisher publisher = content.getPublisher();
        TvAnytimeGenerator delegate = generators.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate;
    }

    private TvAnytimeGenerator fetchDelegateOrThrow(Channel channel) {
        Publisher publisher = channel.getBroadcaster();
        TvAnytimeGenerator delegate = generators.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate;
    }
}
