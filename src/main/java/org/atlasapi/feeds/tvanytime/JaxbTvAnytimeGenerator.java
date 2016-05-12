package org.atlasapi.feeds.tvanytime;

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Content;

import com.google.common.base.Function;
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
import tva.metadata._2010.ServiceInformationTableType;
import tva.metadata._2010.ServiceInformationType;
import tva.metadata._2010.TVAMainType;

import static com.google.common.base.Preconditions.checkNotNull;

public class JaxbTvAnytimeGenerator implements TvAnytimeGenerator {

    private static final String TVA_LANGUAGE = "en-GB";

    private final ObjectFactory factory = new ObjectFactory();
    private final TvAnytimeElementCreator elementCreator;

    public JaxbTvAnytimeGenerator(TvAnytimeElementCreator elementCreator) {
        this.elementCreator = checkNotNull(elementCreator);
    }

    @Override
    public JAXBElement<TVAMainType> generateChannelTVAFrom(Channel channel, Channel parentChannel)
            throws TvaGenerationException {
        try {
            ServiceInformationType serviceInformationElem = elementCreator.createChannelElementFor(channel, parentChannel);
            return createTVAMainFrom(
                    ImmutableSet.<GroupInformationType>of(),
                    ImmutableSet.<ProgramInformationType>of(),
                    ImmutableSet.<BroadcastEventType>of(),
                    ImmutableSet.<OnDemandProgramType>of(),
                    ImmutableSet.of(serviceInformationElem)
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while processing " + channel.getCanonicalUri(), e);
        }
    }

    @Override
    public JAXBElement<TVAMainType> generateMasterbrandTVAFrom(Channel channel)
            throws TvaGenerationException {
        try {
            ServiceInformationType serviceInformationElem = elementCreator.createMasterbrandElementFor(channel);
            return createTVAMainFrom(
                    ImmutableSet.<GroupInformationType>of(),
                    ImmutableSet.<ProgramInformationType>of(),
                    ImmutableSet.<BroadcastEventType>of(),
                    ImmutableSet.<OnDemandProgramType>of(),
                    ImmutableSet.of(serviceInformationElem)
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while processing " + channel.getCanonicalUri(), e);
        }
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
                    ImmutableSet.<OnDemandProgramType>of(),
                    ImmutableSet.<ServiceInformationType>of()
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
                    ImmutableSet.<OnDemandProgramType>of(),
                    ImmutableSet.<ServiceInformationType>of()
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
                    ImmutableSet.<OnDemandProgramType>of(),
                    ImmutableSet.<ServiceInformationType>of()
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
                    ImmutableSet.of(onDemandElem),
                    ImmutableSet.<ServiceInformationType>of()
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
            Iterable<ProgramInformationType> programInformationElems = Iterables.transform(
                    versions.entrySet(), 
                    new Function<Entry<String, ItemAndVersion>, ProgramInformationType>(){
                        @Override
                        public ProgramInformationType apply(Entry<String, ItemAndVersion> input) {
                            return elementCreator.createProgramInformationElementFor(input.getValue(), input.getKey());
                        }
                    }
            );
            Iterable<BroadcastEventType> broadcastElems = Iterables.transform(
                    broadcasts.entrySet(), 
                    new Function<Entry<String, ItemBroadcastHierarchy>, BroadcastEventType>(){
                        @Override
                        public BroadcastEventType apply(Entry<String, ItemBroadcastHierarchy> input) {
                            return elementCreator.createBroadcastEventElementFor(input.getValue(), input.getKey());
                        }
                    }
            );
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
                    ImmutableSet.of(groupInformationElem),
                    programInformationElems,
                    broadcastElems,
                    onDemandElems,
                    ImmutableSet.<ServiceInformationType>of()
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
                    ImmutableSet.<OnDemandProgramType>of(),
                    ImmutableSet.<ServiceInformationType>of()
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
                    ImmutableSet.<OnDemandProgramType>of(),
                    ImmutableSet.<ServiceInformationType>of()
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
                    onDemandElems,
                    ImmutableSet.<ServiceInformationType>of()
            );
        } catch (Exception e) {
            throw new TvaGenerationException("Exception occurred while generating onDemands for " + onDemands, e);
        }
    }

    private JAXBElement<TVAMainType> createTVAMainFrom(Iterable<GroupInformationType> groupInformationElems, 
            Iterable<ProgramInformationType> programInformationElems, Iterable<BroadcastEventType> broadcastElems, 
            Iterable<OnDemandProgramType> onDemandElems, Iterable<ServiceInformationType> channelElems) {
        
            TVAMainType tvaMain = factory.createTVAMainType();
            tvaMain.setLang(TVA_LANGUAGE);

            ProgramDescriptionType progDescription = new ProgramDescriptionType();
            GroupInformationTableType groupInfoTable = factory.createGroupInformationTableType();
            ProgramInformationTableType progInfoTable = factory.createProgramInformationTableType();
            ProgramLocationTableType progLocTable = factory.createProgramLocationTableType();
            ServiceInformationTableType serviceInfoTable = factory.createServiceInformationTableType();
            
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

            for (ServiceInformationType serviceInfo : channelElems) {
                serviceInfoTable.getServiceInformation().add(serviceInfo);
            }

            progDescription.setGroupInformationTable(groupInfoTable);
            progDescription.setProgramInformationTable(progInfoTable);
            progDescription.setProgramLocationTable(progLocTable);
            progDescription.setServiceInformationTable(serviceInfoTable);

            tvaMain.setProgramDescription(progDescription);
            
            return factory.createTVAMain(tvaMain);
    }
}
