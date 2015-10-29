package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Optional;
import org.atlasapi.media.entity.Broadcast;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;
import tva.metadata._2010.*;
import tva.mpeg7._2008.UniqueIDType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RollingWindowBroadcastEventDeduplicatorTest {
    private static final String PCRID = "pcrid";
    private static final String PCRID_AUTHORITY = "pcrid.dmol.co.uk";
    private SentBroadcastEventPcridStore sentBroadcastProgramUrlStore = mock(SentBroadcastEventPcridStore.class);
    private BroadcastEventDeduplicator deduplicator;
    private ObjectFactory factory = new ObjectFactory();
    private JAXBElement<TVAMainType> tvaMain = createTvaMain();

    public RollingWindowBroadcastEventDeduplicatorTest() throws JAXBException {
        this.deduplicator = new RollingWindowBroadcastEventDeduplicator(sentBroadcastProgramUrlStore);
    }

    @Test
    public void testShouldUploadWhen52DaysHavePassed() throws Exception {
        BroadcastEventType broadcastEvent = mock(BroadcastEventType.class);
        CRIDRefType program = mock(CRIDRefType.class);
        String broadcastImi = "broadcastImi";
        String programCrid = "programCrid";
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);

        when(broadcastEvent.getProgram()).thenReturn(program);
        when(program.getCrid()).thenReturn(programCrid);
        when(broadcastEvent.getInstanceMetadataId()).thenReturn(broadcastImi);
        when(sentBroadcastProgramUrlStore.getSentBroadcastEventImi(programCrid, PCRID)).thenReturn(Optional.of(broadcastImi));
        //when(sentBroadcastProgramUrlStore.getSentBroadcastEventTransmissionDate(programCrid, PCRID)).thenReturn(Optional.of(LocalDate.now().minusDays(52)));

        boolean shouldUpload = deduplicator.shouldUpload(bCastTva);

        verify(sentBroadcastProgramUrlStore).getSentBroadcastEventImi(programCrid, PCRID);
        //verify(sentBroadcastProgramUrlStore).getSentBroadcastEventTransmissionDate(programCrid, PCRID);
        verifyNoMoreInteractions(sentBroadcastProgramUrlStore);

        assertEquals(true, shouldUpload);

    }

    @Test
    public void testShouldNotUploadWhen52DaysHaveNotPassed() throws Exception{
        BroadcastEventType broadcastEvent = mock(BroadcastEventType.class);
        CRIDRefType program = mock(CRIDRefType.class);
        String broadcastImi = "broadcastImi";
        String programCrid = "programCrid";
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);

        when(broadcastEvent.getProgram()).thenReturn(program);
        when(program.getCrid()).thenReturn(programCrid);
        when(sentBroadcastProgramUrlStore.getSentBroadcastEventImi(programCrid, PCRID)).thenReturn(Optional.<String>absent());
        //when(sentBroadcastProgramUrlStore.getSentBroadcastEventTransmissionDate(programCrid, PCRID)).thenReturn(Optional.of(LocalDate.now()));

        boolean shouldUpload = deduplicator.shouldUpload(bCastTva);

        verify(sentBroadcastProgramUrlStore).getSentBroadcastEventImi(programCrid, PCRID);
        //verify(sentBroadcastProgramUrlStore).getSentBroadcastEventTransmissionDate(programCrid, PCRID);
        verifyNoMoreInteractions(sentBroadcastProgramUrlStore);

        assertEquals(false, shouldUpload);
    }

    @Test
    public void testRecordUpload() throws Exception{
        Broadcast broadcast = mock(Broadcast.class);
        BroadcastEventType broadcastEvent = mock(BroadcastEventType.class);
        CRIDRefType program = mock(CRIDRefType.class);
        String broadcastImi = "broadcastImi";
        String programCrid = "programCrid";
        Optional<String> broadcastPrid = mock(Optional.class);
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);

        when(broadcastEvent.getProgram()).thenReturn(program);
        when(program.getCrid()).thenReturn(programCrid);
        when(broadcastPrid.isPresent()).thenReturn(true);
        when(broadcastEvent.getInstanceMetadataId()).thenReturn(broadcastImi);
        when(broadcast.getTransmissionTime()).thenReturn(DateTime.now());
        when(broadcastPrid.get()).thenReturn(PCRID);

        deduplicator.recordUpload(bCastTva, broadcast);

        verify(sentBroadcastProgramUrlStore).recordSent(broadcastEvent.getInstanceMetadataId(), broadcast.getTransmissionTime().toLocalDate(), programCrid, PCRID);
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

    private JAXBElement<TVAMainType> createTvaMain(){
        TVAMainType tvaMain = mock(TVAMainType.class);
        return factory.createTVAMain(tvaMain);
    }

}