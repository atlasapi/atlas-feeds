package org.atlasapi.feeds.tvanytime;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.bind.JAXBElement;

import org.atlasapi.media.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class DefaultTvAnytimeGenerator implements TvAnytimeGenerator {

    private static final String TVA_LANGUAGE = "en-GB";

    private final Logger log = LoggerFactory.getLogger(DefaultTvAnytimeGenerator.class);
    private final ObjectFactory factory = new ObjectFactory();
    private final TVAnytimeElementCreator elementCreator;

    public DefaultTvAnytimeGenerator(TVAnytimeElementCreator elementCreator) {
        this.elementCreator = checkNotNull(elementCreator);
    }

    @Override
    public JAXBElement<TVAMainType> generateTVAnytimeFrom(Iterable<Content> contents) {
        
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
