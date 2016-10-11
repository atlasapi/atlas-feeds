package org.atlasapi.feeds.tvanytime;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.atlasapi.feeds.youview.client.ValidationErrorType;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Content;
import org.xml.sax.SAXException;

import tva.metadata._2010.TVAMainType;

import com.google.common.collect.Multimap;


public class ValidatingTvAnytimeGenerator implements TvAnytimeGenerator {
    
    private final TvAnytimeGenerator delegate;
    private final JAXBContext context;
    private final JaxbErrorHandler errorHandler;
    private final Validator validator;
    
    public ValidatingTvAnytimeGenerator(TvAnytimeGenerator delegate) 
            throws SAXException, JAXBException {
        
        this.delegate = checkNotNull(delegate);
        this.context = JAXBContext.newInstance("tva.metadata._2010");
        this.errorHandler = new JaxbErrorHandler();
        this.validator = createValidator();
    }
    
    private Validator createValidator() throws SAXException {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(new File("../atlas-feeds/src/main/resources/tvanytime/youview/youview_metadata_2012-11-19.xsd"));
        return schema.newValidator();
    }

    @Override
    public JAXBElement<TVAMainType> generateChannelTVAFrom(Channel channel, Channel parentChannel)
            throws TvaGenerationException {
        return validate(delegate.generateChannelTVAFrom(channel, parentChannel));
    }

    @Override
    public JAXBElement<TVAMainType> generateMasterbrandTVAFrom(Channel channel)
            throws TvaGenerationException {
        return validate(delegate.generateMasterbrandTVAFrom(channel));
    }

    @Override
    public JAXBElement<TVAMainType> generateContentTVAFrom(Content content)
            throws TvaGenerationException {
        return validate(delegate.generateContentTVAFrom(content));
    }

    @Override
    public JAXBElement<TVAMainType> generateVersionTVAFrom(ItemAndVersion version,
            String versionCrid) throws TvaGenerationException {
        return validate(delegate.generateVersionTVAFrom(version, versionCrid));
    }

    @Override
    public JAXBElement<TVAMainType> generateBroadcastTVAFrom(ItemBroadcastHierarchy broadcast,
            String broadcastImi) throws TvaGenerationException {
        return validate(delegate.generateBroadcastTVAFrom(broadcast, broadcastImi));
    }

    @Override
    public JAXBElement<TVAMainType> generateOnDemandTVAFrom(ItemOnDemandHierarchy onDemand,
            String onDemandImi) throws TvaGenerationException {
        return validate(delegate.generateOnDemandTVAFrom(onDemand, onDemandImi));
    }

    @Override
    public JAXBElement<TVAMainType> generateContentTVAFrom(Content content,
            Map<String, ItemAndVersion> versions, Map<String, ItemBroadcastHierarchy> broadcasts,
            Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException {
        return validate(delegate.generateContentTVAFrom(content, versions, broadcasts, onDemands));
    }

    @Override
    public JAXBElement<TVAMainType> generateVersionTVAFrom(Map<String, ItemAndVersion> versions)
            throws TvaGenerationException {
        return validate(delegate.generateVersionTVAFrom(versions));
    }

    @Override
    public JAXBElement<TVAMainType> generateBroadcastTVAFrom(
            Map<String, ItemBroadcastHierarchy> broadcasts) throws TvaGenerationException {
        return validate(delegate.generateBroadcastTVAFrom(broadcasts));
    }

    @Override
    public JAXBElement<TVAMainType> generateOnDemandTVAFrom(
            Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException {
        return validate(delegate.generateOnDemandTVAFrom(onDemands));
    }

    private Multimap<ValidationErrorType, Exception> validateXml(JAXBElement<TVAMainType> rootElem) 
            throws JAXBException, SAXException, IOException {
        
        validator.validate(new JAXBSource(context, rootElem));
        return errorHandler.errors();
    }
    
    private String combineErrors(Multimap<ValidationErrorType, Exception> validationErrors) {
        StringBuilder errorStr = new StringBuilder();
        for (Entry<ValidationErrorType, Exception> error : validationErrors.entries()) {
            errorStr.append(String.format("%s: %s, %s", error.getKey(), error.getValue().getMessage(), error.getValue()));
        }
        return errorStr.toString();
    }

    private JAXBElement<TVAMainType> validate(JAXBElement<TVAMainType> tvaElem) {
        try {
            Multimap<ValidationErrorType, Exception> validationErrors = validateXml(tvaElem);
            if (!validationErrors.isEmpty()) {
                throw new TvaValidationException(combineErrors(validationErrors));
            }
        } catch(IOException | JAXBException | SAXException e) {
            throw new TvaValidationException("Error Validating TVA " + tvaElem, e);
        }
        
        return tvaElem;
    }
}
