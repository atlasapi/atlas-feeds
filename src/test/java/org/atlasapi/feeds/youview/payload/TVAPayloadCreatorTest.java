package org.atlasapi.feeds.youview.payload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.tvanytime.TvaGenerationException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.persistence.SentBroadcastEventPcridStore;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;
import org.junit.Test;
import org.mockito.Mockito;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.CRIDRefType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.ObjectFactory;
import tva.metadata._2010.ProgramDescriptionType;
import tva.metadata._2010.ProgramLocationTableType;
import tva.metadata._2010.TVAMainType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class TVAPayloadCreatorTest {

    private static final String PAYLOAD = "payload";
    private static final String PCRID = "pcrid";
    private TvAnytimeGenerator generator = mock(TvAnytimeGenerator.class);
    private Converter<JAXBElement<TVAMainType>, String> converter = mock(TVAnytimeStringConverter.class);
    private SentBroadcastEventPcridStore sentBroadcastProgramUrlStore = mock(SentBroadcastEventPcridStore.class);
    private Clock clock = new TimeMachine();
    private ObjectFactory factory = new ObjectFactory();
    private JAXBElement<TVAMainType> tvaMain = createTvaMain();
    
    private final PayloadCreator payloadCreator;
    
    public TVAPayloadCreatorTest() throws JAXBException {
        this.payloadCreator = new TVAPayloadCreator(generator, converter, sentBroadcastProgramUrlStore, clock);
    }
    
    private JAXBElement<TVAMainType> createTvaMain() {
        TVAMainType tvaMain = Mockito.mock(TVAMainType.class);
        return factory.createTVAMain(tvaMain);
    }

    @Test
    public void testContentGeneration() throws PayloadGenerationException, TvaGenerationException {
        Content content = mock(Content.class);
        String contentCrid = "contentCrid";
        
        when(generator.generateContentTVAFrom(content)).thenReturn(tvaMain);
        when(converter.convert(tvaMain)).thenReturn(PAYLOAD);
        
        Payload payload = payloadCreator.payloadFrom(contentCrid, content);

        verify(generator).generateContentTVAFrom(content);
        
        assertEquals(PAYLOAD, payload.payload());
        assertEquals(clock.now(), payload.created());
    }
    
    @Test
    public void testVersionGeneration() throws TvaGenerationException, PayloadGenerationException {
        Item item = mock(Item.class);
        Version version = mock(Version.class);
        ItemAndVersion versionHierarchy = new ItemAndVersion(item, version);
        String versionCrid = "versionCrid";
        
        when(generator.generateVersionTVAFrom(versionHierarchy, versionCrid)).thenReturn(tvaMain);
        when(converter.convert(tvaMain)).thenReturn(PAYLOAD);
        
        Payload payload = payloadCreator.payloadFrom(versionCrid, versionHierarchy);

        verify(generator).generateVersionTVAFrom(versionHierarchy, versionCrid);
        
        assertEquals(PAYLOAD, payload.payload());
        assertEquals(clock.now(), payload.created());
    }
    
    @Test
    public void testOnDemandGeneration() throws TvaGenerationException, PayloadGenerationException {
        Item item = mock(Item.class);
        Version version = mock(Version.class);
        Encoding encoding = mock(Encoding.class);
        Location location = mock(Location.class);
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(item, version, encoding, location);
        String onDemandImi = "onDemandImi";
        
        when(generator.generateOnDemandTVAFrom(onDemandHierarchy, onDemandImi)).thenReturn(tvaMain);
        when(converter.convert(tvaMain)).thenReturn(PAYLOAD);
        
        Payload payload = payloadCreator.payloadFrom(onDemandImi, onDemandHierarchy);

        verify(generator).generateOnDemandTVAFrom(onDemandHierarchy, onDemandImi);
        
        assertEquals(PAYLOAD, payload.payload());
        assertEquals(clock.now(), payload.created());
    }
    
    @Test
    public void testBroadcastGeneratedWhereBroadcastHasNoPCrid() 
            throws TvaGenerationException, PayloadGenerationException {
        
        Item item = mock(Item.class);
        Version version = mock(Version.class);
        Broadcast broadcast = mock(Broadcast.class);
        ItemBroadcastHierarchy broadcastHierarchy = new ItemBroadcastHierarchy(item, version, broadcast, "serviceId");
        String broadcastImi = "broadcastImi";
        
        when(generator.generateBroadcastTVAFrom(broadcastHierarchy, broadcastImi)).thenReturn(tvaMain);
        when(converter.convert(tvaMain)).thenReturn(PAYLOAD);
        when(broadcast.getAliases()).thenReturn(ImmutableSet.<Alias>of());
        
        Optional<Payload> payload = payloadCreator.payloadFrom(broadcastImi, broadcastHierarchy);

        verify(generator).generateBroadcastTVAFrom(broadcastHierarchy, broadcastImi);
        
        assertEquals(PAYLOAD, payload.get().payload());
        assertEquals(clock.now(), payload.get().created());
    }
    
    @Test
    public void testBroadcastGeneratedWhereBroadcastHasPCridAndHasBeenSeenPreviously() 
            throws TvaGenerationException, PayloadGenerationException {
        
        Item item = mock(Item.class);
        Version version = mock(Version.class);
        Broadcast broadcast = mock(Broadcast.class);
        ItemBroadcastHierarchy broadcastHierarchy = new ItemBroadcastHierarchy(item, version, broadcast, "serviceId");
        String broadcastImi = "broadcastImi";
        String programCrid = "programCrid";
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);
        
        when(generator.generateBroadcastTVAFrom(broadcastHierarchy, broadcastImi)).thenReturn(bCastTva);
        when(converter.convert(bCastTva)).thenReturn(PAYLOAD);
        when(broadcast.getAliases()).thenReturn(createPCridAliases());
        when(sentBroadcastProgramUrlStore.getSentBroadcastEventImi(programCrid, PCRID)).thenReturn(Optional.of("1"));
        
        Optional<Payload> payload = payloadCreator.payloadFrom(broadcastImi, broadcastHierarchy);

        verify(generator).generateBroadcastTVAFrom(broadcastHierarchy, broadcastImi);
        
        assertFalse("Broadcast should not be generated if has PCrid and that PCrid has been recorded as seen previously", payload.isPresent());
    }

    @Test
    public void testBroadcastNotGeneratedWhereBroadcastHasPCridButHasNotBeenSeenPreviously() 
            throws TvaGenerationException, PayloadGenerationException {
        
        Item item = mock(Item.class);
        Version version = mock(Version.class);
        Broadcast broadcast = mock(Broadcast.class);
        ItemBroadcastHierarchy broadcastHierarchy = new ItemBroadcastHierarchy(item, version, broadcast, "serviceId");
        String broadcastImi = "broadcastImi";
        String programCrid = "programCrid";
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);
        
        when(generator.generateBroadcastTVAFrom(broadcastHierarchy, broadcastImi)).thenReturn(bCastTva);
        when(converter.convert(bCastTva)).thenReturn(PAYLOAD);
        when(broadcast.getAliases()).thenReturn(createPCridAliases());
        when(sentBroadcastProgramUrlStore.getSentBroadcastEventImi(programCrid, PCRID)).thenReturn(Optional.<String>absent());
        
        Optional<Payload> payload = payloadCreator.payloadFrom(broadcastImi, broadcastHierarchy);

        verify(generator).generateBroadcastTVAFrom(broadcastHierarchy, broadcastImi);
        verify(sentBroadcastProgramUrlStore).recordSent(broadcastImi, programCrid, PCRID);
        
        assertEquals(PAYLOAD, payload.get().payload());
        assertEquals(clock.now(), payload.get().created());
    }
    
    private JAXBElement<TVAMainType> createBroadcastTVAWithPCrid(String pcrid, String crid, String broadcastImi) {
        TVAMainType tvaMain = factory.createTVAMainType();
        
        BroadcastEventType broadcast = new BroadcastEventType();
        broadcast.setInstanceMetadataId(broadcastImi);
        InstanceDescriptionType desc = new InstanceDescriptionType();
        
        UniqueIDType otherId = new UniqueIDType();
        
        otherId.setAuthority("pcrid.dmol.co.uk");
        otherId.setValue(pcrid);
        
        desc.getOtherIdentifier().add(otherId);
        
        CRIDRefType program = new CRIDRefType();
        program.setCrid(crid);
        
        broadcast.setInstanceDescription(desc);
        broadcast.setProgram(program);
        
        ProgramDescriptionType progDescription = new ProgramDescriptionType();
        ProgramLocationTableType progLocTable = factory.createProgramLocationTableType();
        
        progLocTable.getBroadcastEvent().add(broadcast);
        
        progDescription.setProgramLocationTable(progLocTable);
        tvaMain.setProgramDescription(progDescription);
        
        return factory.createTVAMain(tvaMain);
    }

    private Set<Alias> createPCridAliases() {
        return ImmutableSet.of(new Alias("bbc:terrestrial_programme_crid:teleview", PCRID));
    }
}
