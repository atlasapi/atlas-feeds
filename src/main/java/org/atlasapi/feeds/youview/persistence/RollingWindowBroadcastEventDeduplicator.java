package org.atlasapi.feeds.youview.persistence;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import org.atlasapi.media.entity.Broadcast;
import org.joda.time.LocalDate;
import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.TVAMainType;
import tva.mpeg7._2008.UniqueIDType;

import javax.xml.bind.JAXBElement;
import java.util.List;

public class RollingWindowBroadcastEventDeduplicator implements BroadcastEventDeduplicator {
    private static final String PCRID_AUTHORITY = "pcrid.dmol.co.uk";
    private final SentBroadcastEventPcridStore sentBroadcastProgramUrlStore;

    public RollingWindowBroadcastEventDeduplicator(SentBroadcastEventPcridStore sentBroadcastProgramUrlStore){
        this.sentBroadcastProgramUrlStore = sentBroadcastProgramUrlStore;
    }

    @Override
    public boolean shouldUpload(JAXBElement<TVAMainType> tvaElem) {
        BroadcastEventType broadcastEvent = extractBroadcastEvent(tvaElem);
        Optional<String> broadcastPcrid = getPcrid(broadcastEvent);
        String crid = broadcastEvent.getProgram().getCrid();
        String broadcastImi = broadcastEvent.getInstanceMetadataId();

        if (!broadcastPcrid.isPresent()) {
            return true;
        }

        if(broadcastImi.equals(sentBroadcastProgramUrlStore.getSentBroadcastEventImi(crid, broadcastPcrid.get()).get())){
            if(sentBroadcastProgramUrlStore.
                    getSentBroadcastEventTransmissionDate(crid,
                            broadcastPcrid.get()).get().isBefore(LocalDate.now().minusDays(51))) {
                return true;
            }
        }


        return false;
    }

    @Override
    public void recordUpload(JAXBElement<TVAMainType> tvaElem, Broadcast broadcast) {
        BroadcastEventType broadcastEvent = extractBroadcastEvent(tvaElem);
        Optional<String> broadcastPcrid = getPcrid(broadcastEvent);
        String crid = broadcastEvent.getProgram().getCrid();

        if (broadcastPcrid.isPresent()) {
            sentBroadcastProgramUrlStore.recordSent(
                    broadcastEvent.getInstanceMetadataId(),
                    broadcast.getTransmissionTime().toLocalDate(),
                    crid,
                    broadcastPcrid.get()
            );
        }
    }

    private BroadcastEventType extractBroadcastEvent(JAXBElement<TVAMainType> tvaElem) {
        return Iterables.getOnlyElement(
                tvaElem.getValue()
                        .getProgramDescription()
                        .getProgramLocationTable()
                        .getBroadcastEvent()
        );
    }

    private Optional<String> getPcrid(BroadcastEventType broadcastEvent) {
        InstanceDescriptionType instanceDescription = broadcastEvent.getInstanceDescription();
        if (instanceDescription == null) {
            return Optional.absent();
        }
        List<UniqueIDType> otherIdentifier = instanceDescription.getOtherIdentifier();
        if (otherIdentifier == null) {
            return Optional.absent();
        }

        for (UniqueIDType uniqueId : otherIdentifier) {
            if (PCRID_AUTHORITY.equals(uniqueId.getAuthority())) {
                return Optional.of(uniqueId.getValue());
            }
        }
        return Optional.absent();
    }
}
