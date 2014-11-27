package org.atlasapi.feeds.tvanytime;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.bind.JAXBElement;

import org.atlasapi.media.entity.Content;

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

public class JaxbTvAnytimeGenerator implements TvAnytimeGenerator {

    private static final String TVA_LANGUAGE = "en-GB";

    private final ObjectFactory factory = new ObjectFactory();
    private final TvAnytimeElementCreator elementCreator;

    public JaxbTvAnytimeGenerator(TvAnytimeElementCreator elementCreator) {
        this.elementCreator = checkNotNull(elementCreator);
    }

    @Override
    public JAXBElement<TVAMainType> generateTVAnytimeFrom(Content content) throws TvaGenerationException {
        try {
            TVAMainType tvaMain = factory.createTVAMainType();
            elementCreator.permit().reset();
            tvaMain.setLang(TVA_LANGUAGE);

            ProgramDescriptionType progDescription = new ProgramDescriptionType();

            ProgramInformationTableType progInfoTable = factory.createProgramInformationTableType();
            GroupInformationTableType groupInfoTable = factory.createGroupInformationTableType();
            ProgramLocationTableType progLocTable = factory.createProgramLocationTableType();

            for (GroupInformationType groupInfo : elementCreator.createGroupInformationElementsFor(content)) {
                groupInfoTable.getGroupInformation().add(groupInfo);
            }
            for (OnDemandProgramType onDemand : elementCreator.createOnDemandElementsFor(content)) {
                progLocTable.getOnDemandProgram().add(onDemand);
            }
            for (ProgramInformationType progInfo : elementCreator.createProgramInformationElementFor(content)) {
                progInfoTable.getProgramInformation().add(progInfo);
            }
            for (BroadcastEventType broadcast : elementCreator.createBroadcastEventElementsFor(content)) {
                progLocTable.getBroadcastEvent().add(broadcast);
            }

            progDescription.setProgramInformationTable(progInfoTable);
            progDescription.setGroupInformationTable(groupInfoTable);
            progDescription.setProgramLocationTable(progLocTable);

            tvaMain.setProgramDescription(progDescription);
            return factory.createTVAMain(tvaMain);
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while processing " + content.getCanonicalUri(), e);
        }
    }
}
