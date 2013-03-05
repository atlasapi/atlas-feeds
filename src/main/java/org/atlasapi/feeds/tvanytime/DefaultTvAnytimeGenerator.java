package org.atlasapi.feeds.tvanytime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import tva.metadata._2010.GroupInformationTableType;
import tva.metadata._2010.ObjectFactory;
import tva.metadata._2010.ProgramDescriptionType;
import tva.metadata._2010.ProgramInformationTableType;
import tva.metadata._2010.ProgramLocationTableType;
import tva.metadata._2010.ServiceInformationTableType;
import tva.metadata._2010.TVAMainType;

import com.google.common.base.Throwables;
import com.google.inject.internal.Lists;

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

    private final ProgramInformationGenerator progInfoGenerator;
    private final GroupInformationGenerator groupInfoGenerator;
    private final OnDemandLocationGenerator progLocationGenerator;
    private final ServiceInformationGenerator lovefilmServiceInfoGenerator;
    private final ServiceInformationGenerator lovefilmInstantServiceInfoGenerator;
    private final ContentResolver contentResolver;
    private final Logger log = LoggerFactory.getLogger(DefaultTvAnytimeGenerator.class);
    private final boolean performValidation;
    private final ObjectFactory factory = new ObjectFactory();

    public DefaultTvAnytimeGenerator(ProgramInformationGenerator progInfoGenerator, GroupInformationGenerator groupInfoGenerator, OnDemandLocationGenerator progLocationGenerator, ServiceInformationGenerator lovefilmServiceInfoGenerator, ServiceInformationGenerator lovefilmInstantServiceInfoGenerator, ContentResolver contentResolver, boolean performValidation) {
        this.progInfoGenerator = progInfoGenerator;
        this.groupInfoGenerator = groupInfoGenerator;
        this.progLocationGenerator = progLocationGenerator;
        this.lovefilmServiceInfoGenerator = lovefilmServiceInfoGenerator;
        this.lovefilmInstantServiceInfoGenerator = lovefilmInstantServiceInfoGenerator;
        this.contentResolver = contentResolver;
        this.performValidation = performValidation;
    }
    
    @Override
    public void generateXml(Iterable<Item> items, OutputStream outStream, boolean includeServiceInformation) {
        JAXBContext context;
        try {
            context = JAXBContext.newInstance("tva.metadata._2010");
            Marshaller marshaller = context.createMarshaller();
            
            JAXBElement<TVAMainType> rootElem = createXml(items, includeServiceInformation);

            if (performValidation) {
                JAXBSource source = new JAXBSource(context, rootElem);

                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); 
                Schema schema = sf.newSchema(new File("../atlas-feeds/src/main/resources/tvanytime/youview/youview_metadata_2011-07-06.xsd")); 

                Validator validator = schema.newValidator();
                JaxbErrorHandler errorHandler = new JaxbErrorHandler();
                validator.setErrorHandler(errorHandler);

                validator.validate(source);
                
                if(errorHandler.hasErrors()) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    marshaller.marshal(rootElem, os);
                    log.trace("Invalid xml was: {}", os.toString());
                    throw new RuntimeException("XML Validation against schema failed");
                }
                
            }
            
            marshaller.marshal(rootElem, outStream);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private JAXBElement<TVAMainType> createXml(Iterable<Item> items, boolean includeServiceInformation) {
        TVAMainType tvaMain = factory.createTVAMainType();
        tvaMain.setLang(TVA_LANGUAGE);
        
        ProgramDescriptionType progDescription = new ProgramDescriptionType();

        if (includeServiceInformation) {
            ServiceInformationTableType serviceInfoTable = factory.createServiceInformationTableType(); 
            serviceInfoTable.getServiceInformation().add(lovefilmServiceInfoGenerator.generate());
            serviceInfoTable.getServiceInformation().add(lovefilmInstantServiceInfoGenerator.generate());
            progDescription.setServiceInformationTable(serviceInfoTable);
        }

        ProgramInformationTableType progInfoTable = factory.createProgramInformationTableType();
        GroupInformationTableType groupInfoTable = factory.createGroupInformationTableType();
        ProgramLocationTableType progLocTable = factory.createProgramLocationTableType();

        for (Item item : items) {
                progInfoTable.getProgramInformation().add(progInfoGenerator.generate((Item)item));
                progLocTable.getOnDemandProgram().add(progLocationGenerator.generate((Item)item));
                
                if (item instanceof Film) {
                    
                    groupInfoTable.getGroupInformation().add(groupInfoGenerator.generate((Film)item));
                    
                } else if (item instanceof Episode) {
                    
                    Episode episode = (Episode)item;
                    groupInfoTable.getGroupInformation().add(groupInfoGenerator.generate(episode));
                    // TODO to be improved...
                    List<String> parentUris = Lists.newArrayList();
                    if (episode.getSeriesRef() != null) {
                        parentUris.add(episode.getSeriesRef().getUri());
                    }
                    if (episode.getContainer() != null) {
                        parentUris.add(episode.getContainer().getUri());
                    }
                    
                    ResolvedContent seriesAndBrand = contentResolver.findByCanonicalUris(parentUris);
                    
                    if (episode.getSeriesRef() != null) {
                        groupInfoTable.getGroupInformation().add(groupInfoGenerator.generate((Series) seriesAndBrand.get(episode.getSeriesRef().getUri()).requireValue()));   
                    }
                    if (episode.getContainer() != null) {
                        groupInfoTable.getGroupInformation().add(groupInfoGenerator.generate((Brand) seriesAndBrand.get(episode.getContainer().getUri()).requireValue()));
                    }
                }
        }

        progDescription.setProgramInformationTable(progInfoTable);
        progDescription.setGroupInformationTable(groupInfoTable);
        progDescription.setProgramLocationTable(progLocTable);
        
        tvaMain.setProgramDescription(progDescription);
        return factory.createTVAMain(tvaMain);
    }
}
