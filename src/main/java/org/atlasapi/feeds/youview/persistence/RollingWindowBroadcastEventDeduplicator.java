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
 * This class is used to test whether a BroadcastEvent should be sent to YouView while taking into account YouView expiring old associations periodically.
 * BroadcastEvent is used by YouView to link items in the backwards EPG to on-demands.
 * Since YouView key on programme crid, instead of uploading per-broadcast, we're able to only send a single BroadcastEvent per pcrid.
 * The class records when we last uploaded a BroadcastEvent and only suppress if it was sent recently (where recently will be defined).
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
        Optional<BroadcastEventRecord> broadcastEventRecords = Optional.absent();

        if(broadcastEventRecords.isPresent()) {
            broadcastEventRecords = sentBroadcastProgramUrlStore
                                        .getSentBroadcastEventRecords(crid, broadcastPcrid.get());
        } else {
            return false;
        }

        if(!broadcastPcrid.isPresent()) {
            return true;
        }

        if(broadcastImi.equals(broadcastEventRecords.get().getBroadcastEventImi())) {
            return true;
        }

        if(broadcastEventRecords.get()
                .getBroadcastTransmissionDate()
                .isBefore(LocalDate.now().minusDays(DAYS_BROADCAST_LAST_SENT))) {
            return true;
        }

        LOGGER.trace("Broadcast is not uploaded, since it has been last sent for less than " + DAYS_BROADCAST_LAST_SENT + " days or its ProgramURL has already been associated with this service ID and item");
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
