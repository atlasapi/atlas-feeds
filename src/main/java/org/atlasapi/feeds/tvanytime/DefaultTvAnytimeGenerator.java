package org.atlasapi.feeds.tvanytime;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;

import tva.metadata._2010.GroupInformationTableType;
import tva.metadata._2010.ObjectFactory;
import tva.metadata._2010.ProgramDescriptionType;
import tva.metadata._2010.ProgramInformationTableType;
import tva.metadata._2010.ProgramLocationTableType;
import tva.metadata._2010.ServiceInformationTableType;
import tva.metadata._2010.TVAMainType;
import tva.metadata.extended._2010.ExtendedTVAMainType;

import com.google.common.base.Throwables;

public class DefaultTvAnytimeGenerator implements TvAnytimeGenerator {

    private static final String TVA_LANGUAGE = "en-GB";

    private final ProgramInformationGenerator progInfoGenerator;
    private final GroupInformationGenerator groupInfoGenerator;
    private final OnDemandLocationGenerator progLocationGenerator;
    private final ServiceInformationGenerator lovefilmServiceInfoGenerator;
    private final ServiceInformationGenerator lovefilmInstantServiceInfoGenerator;

    public DefaultTvAnytimeGenerator(ProgramInformationGenerator progInfoGenerator, GroupInformationGenerator groupInfoGenerator, OnDemandLocationGenerator progLocationGenerator, ServiceInformationGenerator lovefilmServiceInfoGenerator, ServiceInformationGenerator lovefilmInstantServiceInfoGenerator) {
        this.progInfoGenerator = progInfoGenerator;
        this.groupInfoGenerator = groupInfoGenerator;
        this.progLocationGenerator = progLocationGenerator;
        this.lovefilmServiceInfoGenerator = lovefilmServiceInfoGenerator;
        this.lovefilmInstantServiceInfoGenerator = lovefilmInstantServiceInfoGenerator;
    }
    
    @Override
    public void generateXml(Iterable<Item> items, File file, boolean isBootstrap) {
        // TODO verify package
        JAXBContext context;
        try {
            context = JAXBContext.newInstance("tva.metadata._2010");
            Marshaller marshaller = context.createMarshaller();
            
            ObjectFactory factory = new ObjectFactory();
            TVAMainType tvaMain = factory.createTVAMainType();
            tvaMain.setLang(TVA_LANGUAGE);
            
            ProgramDescriptionType progDescription = new ProgramDescriptionType();

            if (isBootstrap) {
                ServiceInformationTableType serviceInfoTable = new ServiceInformationTableType(); 
                serviceInfoTable.getServiceInformation().add(lovefilmServiceInfoGenerator.generate());
                serviceInfoTable.getServiceInformation().add(lovefilmInstantServiceInfoGenerator.generate());
                progDescription.setServiceInformationTable(serviceInfoTable);
            }

            ProgramInformationTableType progInfoTable = new ProgramInformationTableType();
            GroupInformationTableType groupInfoTable = new GroupInformationTableType();
            ProgramLocationTableType progLocTable = new ProgramLocationTableType();

            for (Item item : items) {
                progInfoTable.getProgramInformation().add(progInfoGenerator.generate(item));
                progLocTable.getOnDemandProgram().add(progLocationGenerator.generate(item));
                if (item instanceof Film) {
                    groupInfoTable.getGroupInformation().add(groupInfoGenerator.generateForFilm((Film)item));
                } else if (item instanceof Episode) {
                    groupInfoTable.getGroupInformation().add(groupInfoGenerator.generateForEpisode((Episode)item));
                    // TODO obtain series/brand and generate for those
                }
            }

            progDescription.setProgramInformationTable(progInfoTable);
            progDescription.setGroupInformationTable(groupInfoTable);
            progDescription.setProgramLocationTable(progLocTable);
            
            tvaMain.setProgramDescription(progDescription);
            JAXBElement<TVAMainType> rootElem = factory.createTVAMain(tvaMain);
            
            marshaller.marshal(rootElem, new FileOutputStream(file));
            
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
