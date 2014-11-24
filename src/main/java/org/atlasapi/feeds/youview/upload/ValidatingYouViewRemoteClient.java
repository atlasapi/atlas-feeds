package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.upload.YouViewResult.failure;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import tva.metadata._2010.TVAMainType;

import com.google.common.collect.Multimap;


public class ValidatingYouViewRemoteClient implements YouViewRemoteClient {

    private final YouViewRemoteClient delegate;
    private final JAXBContext context;
    private final JaxbErrorHandler errorHandler;
    private final Validator validator;

    
    public ValidatingYouViewRemoteClient(YouViewRemoteClient delegate) throws JAXBException, SAXException {
        this.delegate = checkNotNull(delegate);
        this.context = JAXBContext.newInstance("tva.metadata._2010");
        this.errorHandler = new JaxbErrorHandler();
        this.validator = createValidator();
        
        validator.setErrorHandler(errorHandler);
    }
    
    private Validator createValidator() throws SAXException {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(new File("../atlas-feeds/src/main/resources/tvanytime/youview/youview_metadata_2011-07-06.xsd"));
        return schema.newValidator();
    }

    @Override
    public YouViewResult upload(JAXBElement<TVAMainType> tvaElem) {
        try {
            Multimap<ValidationErrorType, Exception> validationErrors = validateXml(tvaElem);
            if (!validationErrors.isEmpty()) {
                return failure(combineErrors(validationErrors));
            }
        } catch(IOException | JAXBException | SAXException e) {
            throw new YouViewRemoteClientException("ErrorValidating TVA " + tvaElem, e);
        }
        
        return delegate.upload(tvaElem);
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

    @Override
    public YouViewResult sendDeleteFor(String remoteId) {
        return delegate.sendDeleteFor(remoteId);
    }

    @Override
    public YouViewResult checkRemoteStatusOf(String transactionId) {
        return delegate.checkRemoteStatusOf(transactionId);
    }

}
