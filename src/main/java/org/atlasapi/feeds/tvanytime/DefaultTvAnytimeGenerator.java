package org.atlasapi.feeds.tvanytime;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.atlasapi.media.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.GroupInformationTableType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.ObjectFactory;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.ProgramDescriptionType;
import tva.metadata._2010.ProgramInformationTableType;
import tva.metadata._2010.ProgramInformationType;
import tva.metadata._2010.ProgramLocationTableType;
import tva.metadata._2010.TVAMainType;

import com.google.common.base.Throwables;

public class DefaultTvAnytimeGenerator implements TvAnytimeGenerator {

    private final class JaxbErrorHandler implements ErrorHandler {
        
        private boolean hasErrors = false;
        
        @Override
        public void warning(SAXParseException e) throws SAXException {
            log.error("XML Validation warning: " + e.getMessage(), e);
            hasErrors = true;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            log.error("XML Validation fatal error: " + e.getMessage(), e);
            hasErrors = true;
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            log.error("XML Validation error: " + e.getMessage(), e);
            hasErrors = true;
        }
        
        public boolean hasErrors() {
            return hasErrors;
        }
    }

    private static final String TVA_LANGUAGE = "en-GB";

    private final Logger log = LoggerFactory.getLogger(DefaultTvAnytimeGenerator.class);
    
    private final ObjectFactory factory = new ObjectFactory();
    
    private final TVAnytimeElementCreator elementCreator;
    private final boolean performValidation;

    public DefaultTvAnytimeGenerator(TVAnytimeElementCreator elementCreator, boolean performValidation) {
        this.elementCreator = checkNotNull(elementCreator);
        this.performValidation = performValidation;
    }
    
    @Override
    public void generateXml(Iterable<Content> contents, OutputStream outStream) {
        try {
            JAXBContext context = JAXBContext.newInstance("tva.metadata._2010");
            Marshaller marshaller = context.createMarshaller();
            
            JAXBElement<TVAMainType> rootElem = createXml(contents);

            if (performValidation) {
                validateXml(context, marshaller, rootElem);
            }
            
            marshaller.marshal(rootElem, outStream);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private void validateXml(JAXBContext context, Marshaller marshaller,
            JAXBElement<TVAMainType> rootElem) throws JAXBException, SAXException, IOException {
        JAXBSource source = new JAXBSource(context, rootElem);

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); 
        Schema schema = sf.newSchema(new File("../atlas-feeds/src/main/resources/tvanytime/youview/youview_metadata_2011-07-06.xsd")); 

        Validator validator = schema.newValidator();
        JaxbErrorHandler errorHandler = new JaxbErrorHandler();
        validator.setErrorHandler(errorHandler);

        validator.validate(source);
        
        if (errorHandler.hasErrors()) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            marshaller.marshal(rootElem, os);
            log.trace("Invalid xml was: {}", os.toString());
            throw new RuntimeException("XML Validation against schema failed");
        }
    }

    private JAXBElement<TVAMainType> createXml(Iterable<Content> contents) {
        
        TVAMainType tvaMain = factory.createTVAMainType();
        elementCreator.permit().reset();
        tvaMain.setLang(TVA_LANGUAGE);
        
        ProgramDescriptionType progDescription = new ProgramDescriptionType();

        ProgramInformationTableType progInfoTable = factory.createProgramInformationTableType();
        GroupInformationTableType groupInfoTable = factory.createGroupInformationTableType();
        ProgramLocationTableType progLocTable = factory.createProgramLocationTableType();

        for (Content content : contents) {
            try {
                for (GroupInformationType groupInfo : elementCreator.createGroupInformationElementsFor(content)) {
                    groupInfoTable.getGroupInformation().add(groupInfo);
                }
                for (OnDemandProgramType onDemand : elementCreator.createOnDemandElementsFor(content)) {
                    progLocTable.getOnDemandProgram().add(onDemand);
                }
                for (ProgramInformationType progInfo : elementCreator.createProgramInformationElementFor(content).asSet()) {
                    progInfoTable.getProgramInformation().add(progInfo);
                }
                for (BroadcastEventType broadcast : elementCreator.createBroadcastEventElementsFor(content)) {
                    progLocTable.getBroadcastEvent().add(broadcast);
                }
            } catch (Exception e) {
                log.error("Exception occurred while processing " + content.getCanonicalUri() + " " + e.getMessage(), e);
            }
        }

        progDescription.setProgramInformationTable(progInfoTable);
        progDescription.setGroupInformationTable(groupInfoTable);
        progDescription.setProgramLocationTable(progLocTable);
        
        tvaMain.setProgramDescription(progDescription);
        return factory.createTVAMain(tvaMain);
    }
}
