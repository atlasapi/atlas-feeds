package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import org.atlasapi.media.entity.Broadcast;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.InstanceDescriptionType;
import tva.metadata._2010.TVAMainType;
import tva.mpeg7._2008.UniqueIDType;

import javax.xml.bind.JAXBElement;
import java.util.List;

/**
 * This class is used to
 */
public class RollingWindowBroadcastEventDeduplicator implements BroadcastEventDeduplicator {
    private static final String PCRID_AUTHORITY = "pcrid.dmol.co.uk";
    private static final int DAYS_BROADCAST_LAST_SENT = 51;
    private static final Logger LOGGER = LoggerFactory.getLogger(RollingWindowBroadcastEventDeduplicator.class);

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
        Optional<BroadcastEventRecords> broadcastEventRecords = sentBroadcastProgramUrlStore
                                                                    .getSentBroadcastEventImi(crid, broadcastPcrid.get());

        if (!broadcastPcrid.isPresent()) {
            return true;
        }

        if(broadcastImi.equals(broadcastEventRecords.get().getBroadcastEventImi())){
            return true;
        }

        if(broadcastEventRecords.get().getBroadcastTransmissionDate().isBefore(LocalDate.now().minusDays(DAYS_BROADCAST_LAST_SENT))) {
            return true;
        }
        LOGGER.trace("Broadcast is not uploaded, since it has been last sent for less than 52 days or its ProgramURL has already been associated with this service ID and item");
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
