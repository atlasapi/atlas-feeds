package org.atlasapi.feeds.youview.payload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvaGenerationException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.persistence.SentBroadcastEventPcridStore;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.TVAMainType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.Clock;


public class TVAPayloadCreator implements PayloadCreator {
    
    private static final String TERRESTRIAL_PROGRAMME_CRID_NS = "bbc:terrestrial_programme_crid:teleview";
    private static final String PCRID_AUTHORITY = "pcrid.dmol.co.uk";
    
    private final Logger log = LoggerFactory.getLogger(TVAPayloadCreator.class);
    private final TvAnytimeGenerator generator;
    private final Converter<JAXBElement<TVAMainType>, String> converter;
    private final SentBroadcastEventPcridStore sentBroadcastProgramUrlStore;
    private final Clock clock;
    
    public TVAPayloadCreator(TvAnytimeGenerator generator, 
            Converter<JAXBElement<TVAMainType>, String> converter, 
            SentBroadcastEventPcridStore sentBroadcastProgramUrlStore, Clock clock) 
                    throws JAXBException {
        
        this.generator = checkNotNull(generator);
        this.converter = checkNotNull(converter);
        this.sentBroadcastProgramUrlStore = checkNotNull(sentBroadcastProgramUrlStore);
        this.clock = checkNotNull(clock);
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
                if (alreadyUploaded(tvaElem)) {
                    log.trace("Not uploading broadcast, since its ProgramURL has already been associated with this service ID and item");
                    return Optional.absent();
                } else {
                    recordUpload(tvaElem);
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
    
    private void recordUpload(JAXBElement<TVAMainType> tvaElem) {
        BroadcastEventType broadcastEvent = extractBroadcastEvent(tvaElem);
        Optional<String> broadcastPcrid = getPcrid(broadcastEvent);
        String crid = broadcastEvent.getProgram().getCrid();
        if (broadcastPcrid.isPresent()) {
            sentBroadcastProgramUrlStore.recordSent(
                    broadcastEvent.getInstanceMetadataId(),
                    crid, 
                    broadcastPcrid.get()
            );
        }
    }

    private boolean alreadyUploaded(JAXBElement<TVAMainType> tvaElem) {
        BroadcastEventType broadcastEvent = extractBroadcastEvent(tvaElem);
        Optional<String> broadcastPcrid = getPcrid(broadcastEvent);
        String crid = broadcastEvent.getProgram().getCrid();
        if (!broadcastPcrid.isPresent()) {
            return false;
        }
        
        return sentBroadcastProgramUrlStore.getSentBroadcastEventImi(crid, broadcastPcrid.get())
                                           .isPresent();
    }

    private BroadcastEventType extractBroadcastEvent(JAXBElement<TVAMainType> tvaElem) {
        return Iterables.getOnlyElement(
                tvaElem.getValue()
                        .getProgramDescription()
                        .getProgramLocationTable()
                        .getBroadcastEvent()
        );
    }

    private Optional<String> getPcrid(BroadcastEventType broadcastEvent) {
        InstanceDescriptionType instanceDescription = broadcastEvent.getInstanceDescription();
        if (instanceDescription == null) {
            return Optional.absent();
        }
        List<UniqueIDType> otherIdentifier = instanceDescription.getOtherIdentifier();
        if (otherIdentifier == null) {
            return Optional.absent();
        }
        
        for (UniqueIDType uniqueId : otherIdentifier) {
            if (PCRID_AUTHORITY.equals(uniqueId.getAuthority())) {
                return Optional.of(uniqueId.getValue());
            }
        }
        return Optional.absent();
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
