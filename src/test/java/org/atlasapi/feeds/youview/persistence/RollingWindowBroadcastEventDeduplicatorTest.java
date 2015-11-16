package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Optional;
import org.atlasapi.media.entity.Broadcast;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;
import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.CRIDRefType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.ObjectFactory;
import tva.metadata._2010.ProgramDescriptionType;
import tva.metadata._2010.ProgramLocationTableType;
import tva.metadata._2010.TVAMainType;
import tva.mpeg7._2008.UniqueIDType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RollingWindowBroadcastEventDeduplicatorTest {
    private static final String PCRID = "pcrid";
    private SentBroadcastEventPcridStore sentBroadcastProgramUrlStore = mock(SentBroadcastEventPcridStore.class);
    private BroadcastEventDeduplicator deduplicator;
    private ObjectFactory factory = new ObjectFactory();
    private JAXBElement<TVAMainType> tvaMain = createTvaMain();

    public RollingWindowBroadcastEventDeduplicatorTest() throws JAXBException {
        this.deduplicator = new RollingWindowBroadcastEventDeduplicator(sentBroadcastProgramUrlStore);
    }

    @Test
    public void testShouldUploadWhenBroadcastEventRecordIsPresent() {
        BroadcastEventType broadcastEvent = mock(BroadcastEventType.class);
        CRIDRefType program = mock(CRIDRefType.class);
        String broadcastImi = "broadcastImi";
        String programCrid = "programCrid";
        BroadcastEventRecord broadcastEventRecord = mock(BroadcastEventRecord.class);
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);

        when(broadcastEvent.getProgram()).thenReturn(program);
        when(program.getCrid()).thenReturn(programCrid);
        when(broadcastEvent.getInstanceMetadataId()).thenReturn(broadcastImi);
        when(sentBroadcastProgramUrlStore.getSentBroadcastEventRecords(programCrid, PCRID)).thenReturn(Optional.of(broadcastEventRecord));
        when(broadcastEventRecord.getBroadcastTransmissionDate()).thenReturn(LocalDate.now().minusDays(52));

        boolean shouldUpload = deduplicator.shouldUpload(bCastTva);

        assertEquals(shouldUpload, true);
    }

    @Test
    public void testShouldUploadWhen52DaysHavePassed() throws Exception {
        BroadcastEventType broadcastEvent = mock(BroadcastEventType.class);
        CRIDRefType program = mock(CRIDRefType.class);
        String broadcastImi = "broadcastImi";
        String nonMatchingBroadcastImi = "nonMatchingBroadcastImi";
        String programCrid = "programCrid";
        BroadcastEventRecord broadcastEventRecord = mock(BroadcastEventRecord.class);
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);

        when(broadcastEvent.getProgram()).thenReturn(program);
        when(program.getCrid()).thenReturn(programCrid);
        when(broadcastEvent.getInstanceMetadataId()).thenReturn(broadcastImi);
        when(sentBroadcastProgramUrlStore.getSentBroadcastEventRecords(programCrid, PCRID)).thenReturn(Optional.of(broadcastEventRecord));
        when(broadcastEventRecord.getBroadcastEventImi()).thenReturn(nonMatchingBroadcastImi);
        when(broadcastEventRecord.getBroadcastTransmissionDate()).thenReturn(LocalDate.now().minusDays(52));

        boolean shouldUpload = deduplicator.shouldUpload(bCastTva);

        verify(sentBroadcastProgramUrlStore).getSentBroadcastEventRecords(programCrid, PCRID);
        verify(broadcastEventRecord).getBroadcastEventImi();
        verify(broadcastEventRecord).getBroadcastTransmissionDate();
        verifyNoMoreInteractions(sentBroadcastProgramUrlStore);

        assertEquals(true, shouldUpload);

    }

    @Test
    public void testShouldNotUploadWhen52DaysHaveNotPassed() throws Exception{
        BroadcastEventType broadcastEvent = mock(BroadcastEventType.class);
        CRIDRefType program = mock(CRIDRefType.class);
        String broadcastImi = "broadcastImi";
        String nonMatchingBroadcastImi = "nonMatchingBroadcastImi";
        String programCrid = "programCrid";
        BroadcastEventRecord broadcastEventRecord = mock(BroadcastEventRecord.class);
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);

        when(broadcastEvent.getProgram()).thenReturn(program);
        when(program.getCrid()).thenReturn(programCrid);
        when(broadcastEvent.getInstanceMetadataId()).thenReturn(broadcastImi);
        when(sentBroadcastProgramUrlStore.getSentBroadcastEventRecords(programCrid, PCRID)).thenReturn(Optional.of(broadcastEventRecord));
        when(broadcastEventRecord.getBroadcastEventImi()).thenReturn(nonMatchingBroadcastImi);
        when(broadcastEventRecord.getBroadcastTransmissionDate()).thenReturn(LocalDate.now().minusDays(51));

        boolean shouldUpload = deduplicator.shouldUpload(bCastTva);

        verify(sentBroadcastProgramUrlStore).getSentBroadcastEventRecords(programCrid, PCRID);
        verify(broadcastEventRecord).getBroadcastEventImi();
        verify(broadcastEventRecord).getBroadcastTransmissionDate();
        verifyNoMoreInteractions(sentBroadcastProgramUrlStore);

        assertEquals(false, shouldUpload);
    }

    @Test
    public void testShouldUploadWhenBroadcastImiMatches() throws Exception{
        BroadcastEventType broadcastEvent = mock(BroadcastEventType.class);
        CRIDRefType program = mock(CRIDRefType.class);
        String broadcastImi = "broadcastImi";
        String programCrid = "programCrid";
        BroadcastEventRecord broadcastEventRecord = mock(BroadcastEventRecord.class);
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);

        when(broadcastEvent.getProgram()).thenReturn(program);
        when(program.getCrid()).thenReturn(programCrid);
        when(broadcastEvent.getInstanceMetadataId()).thenReturn(broadcastImi);
        when(sentBroadcastProgramUrlStore.getSentBroadcastEventRecords(programCrid, PCRID)).thenReturn(Optional.of(broadcastEventRecord));
        when(broadcastEventRecord.getBroadcastEventImi()).thenReturn(broadcastImi);
        when(broadcastEventRecord.getBroadcastTransmissionDate()).thenReturn(LocalDate.now().minusDays(51));

        boolean shouldUpload = deduplicator.shouldUpload(bCastTva);

        verify(sentBroadcastProgramUrlStore).getSentBroadcastEventRecords(programCrid, PCRID);
        verify(broadcastEventRecord).getBroadcastEventImi();
        verifyNoMoreInteractions(sentBroadcastProgramUrlStore);

        assertEquals(true, shouldUpload);
    }

    @Test
    public void testShouldNotUploadWhenBroadcastImiDoesNotMatches() throws Exception{
        BroadcastEventType broadcastEvent = mock(BroadcastEventType.class);
        CRIDRefType program = mock(CRIDRefType.class);
        String broadcastImi = "broadcastImi";
        String nonMatchingBroadcastImi = "nonMatchingBroadcastImi";
        String programCrid = "programCrid";
        BroadcastEventRecord broadcastEventRecord = mock(BroadcastEventRecord.class);
        JAXBElement<TVAMainType> bCastTva = createBroadcastTVAWithPCrid(PCRID, programCrid, broadcastImi);

        when(broadcastEvent.getProgram()).thenReturn(program);
        when(program.getCrid()).thenReturn(programCrid);
        when(broadcastEvent.getInstanceMetadataId()).thenReturn(broadcastImi);
        when(sentBroadcastProgramUrlStore.getSentBroadcastEventRecords(programCrid, PCRID)).thenReturn(Optional.of(broadcastEventRecord));
        when(broadcastEventRecord.getBroadcastEventImi()).thenReturn(nonMatchingBroadcastImi);
        when(broadcastEventRecord.getBroadcastTransmissionDate()).thenReturn(LocalDate.now().minusDays(51));

        boolean shouldUpload = deduplicator.shouldUpload(bCastTva);

        verify(sentBroadcastProgramUrlStore).getSentBroadcastEventRecords(programCrid, PCRID);
        verify(broadcastEventRecord).getBroadcastEventImi();
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