package org.atlasapi.feeds.youview.payload;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.tvanytime.TvaGenerationException;
import org.atlasapi.feeds.tvanytime.granular.GranularTvAnytimeGenerator;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.tasks.Payload;
import org.atlasapi.media.entity.Content;

import tva.metadata._2010.TVAMainType;

import com.metabroadcast.common.time.Clock;


public class TVAPayloadCreator implements PayloadCreator {
    
    private final GranularTvAnytimeGenerator generator;
    private final Converter<JAXBElement<TVAMainType>, String> converter;
    private final Clock clock;
    
    public TVAPayloadCreator(GranularTvAnytimeGenerator generator, 
            Converter<JAXBElement<TVAMainType>, String> converter, Clock clock) 
                    throws JAXBException {
        
        this.generator = checkNotNull(generator);
        this.converter = checkNotNull(converter);
        this.clock = checkNotNull(clock);
    }

    @Override
    public Payload createFrom(Content content) throws PayloadGenerationException {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateContentTVAFrom(content);
            return new Payload(converter.convert(tvaElem), clock.now());
        } catch (TvaGenerationException e) {
            throw new PayloadGenerationException(e);
        }
    }

    @Override
    public Payload createFrom(String versionCrid, ItemAndVersion versionHierarchy) throws PayloadGenerationException {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateVersionTVAFrom(versionHierarchy, versionCrid);
            return new Payload(converter.convert(tvaElem), clock.now());
        } catch (TvaGenerationException e) {
            throw new PayloadGenerationException(e);
        }
    }

    @Override
    public Payload createFrom(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy) throws PayloadGenerationException {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateBroadcastTVAFrom(broadcastHierarchy, broadcastImi);
            return new Payload(converter.convert(tvaElem), clock.now());
        } catch (TvaGenerationException e) {
            throw new PayloadGenerationException(e);
        }
    }

    @Override
    public Payload createFrom(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy) throws PayloadGenerationException {
        try {
            JAXBElement<TVAMainType> tvaElem = generator.generateOnDemandTVAFrom(onDemandHierarchy, onDemandImi);
            return new Payload(converter.convert(tvaElem), clock.now());
        } catch (TvaGenerationException e) {
            throw new PayloadGenerationException(e);
        }
    }
}
