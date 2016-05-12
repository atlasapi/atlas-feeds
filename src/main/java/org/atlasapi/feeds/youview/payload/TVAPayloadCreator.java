package org.atlasapi.feeds.youview.payload;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvaGenerationException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.persistence.BroadcastEventDeduplicator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.metadata._2010.TVAMainType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.time.Clock;

public class TVAPayloadCreator implements PayloadCreator {
    
    private static final String TERRESTRIAL_PROGRAMME_CRID_NS = "bbc:terrestrial_programme_crid:teleview";
    
    private final Logger log = LoggerFactory.getLogger(TVAPayloadCreator.class);
    private final TvAnytimeGenerator generator;
    private final ChannelResolver channelResolver;
    private final Converter<JAXBElement<TVAMainType>, String> converter;
    private final BroadcastEventDeduplicator broadcastEventDeduplicator;
    private final Clock clock;
    
    public TVAPayloadCreator(TvAnytimeGenerator generator, ChannelResolver channelResolver,
            Converter<JAXBElement<TVAMainType>, String> converter, 
            BroadcastEventDeduplicator broadcastEventDeduplicator, Clock clock)
                    throws JAXBException {

        this.channelResolver = checkNotNull(channelResolver);
        this.generator = checkNotNull(generator);
        this.converter = checkNotNull(converter);
        this.broadcastEventDeduplicator = checkNotNull(broadcastEventDeduplicator);
        this.clock = checkNotNull(clock);
    }

    @Override
    public Payload payloadFrom(Channel channel, boolean masterbrand)
            throws PayloadGenerationException {
        try {
            JAXBElement<TVAMainType> tvaElem;
            if (masterbrand) {
                tvaElem = generator.generateMasterbrandTVAFrom(channel);
            } else {
                Maybe<Channel> channelParent = channelResolver.fromId(channel.getParent());
                tvaElem = generator.generateChannelTVAFrom(channel, channelParent.requireValue());
            }
            return new Payload(converter.convert(tvaElem), clock.now());
        } catch (TvaGenerationException e) {
            throw new PayloadGenerationException(e);
        }
    }

    @Override
    public Payload payloadFrom(String contentCrid, Content content) throws PayloadGenerationException {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateContentTVAFrom(content);
            return new Payload(converter.convert(tvaElem), clock.now());
        } catch (TvaGenerationException e) {
            throw new PayloadGenerationException(e);
        }
    }

    @Override
    public Payload payloadFrom(String versionCrid, ItemAndVersion versionHierarchy) throws PayloadGenerationException {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateVersionTVAFrom(versionHierarchy, versionCrid);
            return new Payload(converter.convert(tvaElem), clock.now());
        } catch (TvaGenerationException e) {
            throw new PayloadGenerationException(e);
        }
    }

    @Override
    public Optional<Payload> payloadFrom(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy) throws PayloadGenerationException {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateBroadcastTVAFrom(broadcastHierarchy, broadcastImi);
            
            if (hasPCrid(broadcastHierarchy.broadcast())) {
                if (broadcastEventDeduplicator.shouldUpload(tvaElem)) {
                    broadcastEventDeduplicator.recordUpload(tvaElem, broadcastHierarchy.broadcast());
                } else {
                    return Optional.absent();
                }
            }
            
            return Optional.of(new Payload(converter.convert(tvaElem), clock.now()));
        } catch (TvaGenerationException e) {
            throw new PayloadGenerationException(e);
        }
    }
    
    private static boolean hasPCrid(Broadcast broadcast) {
        return Iterables.tryFind(broadcast.getAliases(), new Predicate<Alias>() {
            @Override
            public boolean apply(Alias input) {
                return TERRESTRIAL_PROGRAMME_CRID_NS.equals(input.getNamespace());
            }
        }).isPresent();
    }

    @Override
    public Payload payloadFrom(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy) throws PayloadGenerationException {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateOnDemandTVAFrom(onDemandHierarchy, onDemandImi);
            return new Payload(converter.convert(tvaElem), clock.now());
        } catch (TvaGenerationException e) {
            throw new PayloadGenerationException(e);
        }
    }
}
