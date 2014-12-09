package org.atlasapi.feeds.youview.persistence;

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
public interface SentBroadcastEventProgramUrlStore {

    boolean beenSent(String crid, String programUrl, String serviceIdRef);
    
    void removeSentRecord(String crid, String programUrl, String serviceIdRef);

    void recordSent(String crid, String programUrl, String serviceIdRef);

}