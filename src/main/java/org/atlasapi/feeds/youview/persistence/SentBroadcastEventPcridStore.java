package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Optional;
import org.joda.time.LocalDate;

/**
 * A store to record that a programUrl reference has been sent to YouView 
 * for a particular content crid, on a given service. 
 * 
 * YouView only required that this record be sent a single time, so the
 * reference is known. Therefore this store can used to record which have
 * been store.
 * 
 * @author tom
 *
 */
public interface SentBroadcastEventPcridStore {

    Optional<BroadcastEventRecords> getSentBroadcastEventImi(String itemCrid, String pcrid);
    
    void removeSentRecord(String crid, String pcrid);

    void recordSent(String broadcasteEventImi, LocalDate transmissionDate, String contentCrid, String programmeCrid);
}