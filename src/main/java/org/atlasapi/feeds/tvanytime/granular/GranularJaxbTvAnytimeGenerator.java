package org.atlasapi.feeds.tvanytime.granular;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.tvanytime.TvaGenerationException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

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

public class GranularJaxbTvAnytimeGenerator implements GranularTvAnytimeGenerator {

    private static final Logger log = LoggerFactory.getLogger(GranularJaxbTvAnytimeGenerator.class);
    
    private static final String TVA_LANGUAGE = "en-GB";

    private final ObjectFactory factory = new ObjectFactory();
    private final GranularTvAnytimeElementCreator elementCreator;

    public GranularJaxbTvAnytimeGenerator(GranularTvAnytimeElementCreator elementCreator) {
        this.elementCreator = checkNotNull(elementCreator);
    }

    @Override
    public JAXBElement<TVAMainType> generateContentTVAFrom(Content content)
            throws TvaGenerationException {
        try {
            GroupInformationType groupInformationElem = elementCreator.createGroupInformationElementFor(content);
            return createTVAMainFrom(
                    ImmutableSet.of(groupInformationElem),
                    ImmutableSet.<ProgramInformationType>of(),
                    ImmutableSet.<BroadcastEventType>of(),
                    ImmutableSet.<OnDemandProgramType>of()
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while processing " + content.getCanonicalUri(), e);
        }
    }

    @Override
    public JAXBElement<TVAMainType> generateVersionTVAFrom(ItemAndVersion version, String versionCrid) 
            throws TvaGenerationException {
        try {
            ProgramInformationType programInformationElem = elementCreator.createProgramInformationElementFor(version, versionCrid);
            return createTVAMainFrom(
                    ImmutableSet.<GroupInformationType>of(),
                    ImmutableSet.of(programInformationElem),
                    ImmutableSet.<BroadcastEventType>of(),
                    ImmutableSet.<OnDemandProgramType>of()
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while generating version for " + version, e);
        }
    }

    @Override
    public JAXBElement<TVAMainType> generateBroadcastTVAFrom(ItemBroadcastHierarchy broadcast, String broadcastImi) 
            throws TvaGenerationException {
        try {
            BroadcastEventType broadcastElem = elementCreator.createBroadcastEventElementFor(broadcast, broadcastImi);
            return createTVAMainFrom(
                    ImmutableSet.<GroupInformationType>of(),
                    ImmutableSet.<ProgramInformationType>of(),
                    ImmutableSet.of(broadcastElem),
                    ImmutableSet.<OnDemandProgramType>of()
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while generating broadcast for " + broadcast, e);
        }
    }

    @Override
    public JAXBElement<TVAMainType> generateOnDemandTVAFrom(ItemOnDemandHierarchy onDemand, String onDemandImi) 
            throws TvaGenerationException {
        try {
            OnDemandProgramType onDemandElem = elementCreator.createOnDemandElementFor(onDemand, onDemandImi);
            return createTVAMainFrom(
                    ImmutableSet.<GroupInformationType>of(),
                    ImmutableSet.<ProgramInformationType>of(),
                    ImmutableSet.<BroadcastEventType>of(),
                    ImmutableSet.of(onDemandElem)
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while generating on-demand for " + onDemand, e);
        }
    }

    @Override
    public JAXBElement<TVAMainType> generateContentTVAFrom(Content content,
            Map<String, ItemAndVersion> versions, Map<String, ItemBroadcastHierarchy> broadcasts,
            Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException {
        try {
            GroupInformationType groupInformationElem = elementCreator.createGroupInformationElementFor(content);
            Iterable<ProgramInformationType> programInformationElems = Iterables.filter(Iterables.transform(
                    versions.entrySet(), 
                    new Function<Entry<String, ItemAndVersion>, ProgramInformationType>(){
                        @Override
                        public ProgramInformationType apply(Entry<String, ItemAndVersion> input) {
                            try {
                                return elementCreator.createProgramInformationElementFor(input.getValue(), input.getKey());
                            } catch (Exception e) {
                                log.error("Failed to generate programme information for item {} version {}", 
                                        input.getValue().item().getCanonicalUri(), input.getValue().version().getCanonicalUri());
                                return null;
                            }
                        }
                    }
            ), Predicates.notNull());
            
            Iterable<BroadcastEventType> broadcastElems = Iterables.filter(Iterables.transform(
                    broadcasts.entrySet(), 
                    new Function<Entry<String, ItemBroadcastHierarchy>, BroadcastEventType>(){
                        @Override
                        public BroadcastEventType apply(Entry<String, ItemBroadcastHierarchy> input) {
                            try {
                                return elementCreator.createBroadcastEventElementFor(input.getValue(), input.getKey());
                            } catch (Exception e) {
                                log.error("Failed to generate programme information for item {} broadcast {}", 
                                        input.getValue().item().getCanonicalUri(), input.getValue().broadcast().getSourceId());
                                return null;
                            }
                        }
                    }
            ), Predicates.notNull());
            
            Iterable<OnDemandProgramType> onDemandElems = Iterables.filter(Iterables.transform(
                    onDemands.entrySet(), 
                    new Function<Entry<String, ItemOnDemandHierarchy>, OnDemandProgramType>(){
                        @Override
                        public OnDemandProgramType apply(Entry<String, ItemOnDemandHierarchy> input) {
                            try {
                                return elementCreator.createOnDemandElementFor(input.getValue(), input.getKey());
                            } catch (Exception e) {
                                log.error("Failed to generate programme information for item {} location {}", 
                                        input.getValue().item().getCanonicalUri(), input.getValue().location().getUri());
                                return null;
                            }
                        }
                    }
            ), Predicates.notNull());
            
            return createTVAMainFrom(
                    ImmutableSet.of(groupInformationElem),
                    programInformationElems,
                    broadcastElems,
                    onDemandElems
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while generating broadcast for " + versions, e);
        }
    }

    @Override
    public JAXBElement<TVAMainType> generateVersionTVAFrom(Map<String, ItemAndVersion> versions)
            throws TvaGenerationException {
        try {
            Iterable<ProgramInformationType> programInformationElems = Iterables.transform(
                    versions.entrySet(), 
                    new Function<Entry<String, ItemAndVersion>, ProgramInformationType>(){
                        @Override
                        public ProgramInformationType apply(Entry<String, ItemAndVersion> input) {
                            return elementCreator.createProgramInformationElementFor(input.getValue(), input.getKey());
                        }
                    }
            );
            return createTVAMainFrom(
                    ImmutableSet.<GroupInformationType>of(),
                    programInformationElems,
                    ImmutableSet.<BroadcastEventType>of(),
                    ImmutableSet.<OnDemandProgramType>of()
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while generating broadcast for " + versions, e);
        }
    }

    @Override
    public JAXBElement<TVAMainType> generateBroadcastTVAFrom(
            Map<String, ItemBroadcastHierarchy> broadcasts) throws TvaGenerationException {
        try {
            Iterable<BroadcastEventType> broadcastElems = Iterables.transform(
                    broadcasts.entrySet(), 
                    new Function<Entry<String, ItemBroadcastHierarchy>, BroadcastEventType>(){
                        @Override
                        public BroadcastEventType apply(Entry<String, ItemBroadcastHierarchy> input) {
                            return elementCreator.createBroadcastEventElementFor(input.getValue(), input.getKey());
                        }
                    }
            );
            return createTVAMainFrom(
                    ImmutableSet.<GroupInformationType>of(),
                    ImmutableSet.<ProgramInformationType>of(),
                    broadcastElems,
                    ImmutableSet.<OnDemandProgramType>of()
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while generating broadcast for " + broadcasts, e);
        }
    }

    @Override
    public JAXBElement<TVAMainType> generateOnDemandTVAFrom(
            Map<String, ItemOnDemandHierarchy> onDemands) throws TvaGenerationException {
        try {
            Iterable<OnDemandProgramType> onDemandElems = Iterables.transform(
                    onDemands.entrySet(), 
                    new Function<Entry<String, ItemOnDemandHierarchy>, OnDemandProgramType>(){
                        @Override
                        public OnDemandProgramType apply(Entry<String, ItemOnDemandHierarchy> input) {
                            return elementCreator.createOnDemandElementFor(input.getValue(), input.getKey());
                        }
                    }
            );
            return createTVAMainFrom(
                    ImmutableSet.<GroupInformationType>of(),
                    ImmutableSet.<ProgramInformationType>of(),
                    ImmutableSet.<BroadcastEventType>of(),
                    onDemandElems
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while generating onDemands for " + onDemands, e);
        }
    }

    private JAXBElement<TVAMainType> createTVAMainFrom(Iterable<GroupInformationType> groupInformationElems, 
            Iterable<ProgramInformationType> programInformationElems, Iterable<BroadcastEventType> broadcastElems, 
            Iterable<OnDemandProgramType> onDemandElems) {
        
            TVAMainType tvaMain = factory.createTVAMainType();
            tvaMain.setLang(TVA_LANGUAGE);

            ProgramDescriptionType progDescription = new ProgramDescriptionType();
            GroupInformationTableType groupInfoTable = factory.createGroupInformationTableType();
            ProgramInformationTableType progInfoTable = factory.createProgramInformationTableType();
            ProgramLocationTableType progLocTable = factory.createProgramLocationTableType();
            
            for (GroupInformationType groupInfo : groupInformationElems) {
                groupInfoTable.getGroupInformation().add(groupInfo);
            }
            
            for (ProgramInformationType progInfo : programInformationElems) {
                progInfoTable.getProgramInformation().add(progInfo);
            }

            for (BroadcastEventType broadcast : broadcastElems) {
                progLocTable.getBroadcastEvent().add(broadcast);
            }
            
            for (OnDemandProgramType onDemand : onDemandElems) {
                progLocTable.getOnDemandProgram().add(onDemand);
            }

            progDescription.setGroupInformationTable(groupInfoTable);
            progDescription.setProgramInformationTable(progInfoTable);
            progDescription.setProgramLocationTable(progLocTable);

            tvaMain.setProgramDescription(progDescription);
            
            return factory.createTVAMain(tvaMain);
    }
}
