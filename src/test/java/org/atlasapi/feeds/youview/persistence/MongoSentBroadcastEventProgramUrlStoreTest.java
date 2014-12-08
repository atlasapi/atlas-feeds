package org.atlasapi.feeds.youview.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.atlasapi.feeds.youview.persistence.MongoSentBroadcastEventProgramUrlStore;
import org.junit.Before;
import org.junit.Test;

import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class MongoSentBroadcastEventProgramUrlStoreTest {

    private DatabasedMongo mongo;
    private MongoSentBroadcastEventProgramUrlStore sentBroadcastProgramUrlStore;
    
    @Before
    public void setUp() {
        mongo = MongoTestHelper.anEmptyTestDatabase();
        sentBroadcastProgramUrlStore = new MongoSentBroadcastEventProgramUrlStore(mongo);
    }
    
    @Test
    public void testStoreAndRemove() {
        String crid = "crid1";
        String programUrl = "http://example.org/1";
        String serviceIdRef = "1234";
        
        assertFalse(sentBroadcastProgramUrlStore.beenSent(crid, programUrl, serviceIdRef));
            
        sentBroadcastProgramUrlStore.recordSent(crid, programUrl, serviceIdRef);
        assertTrue(sentBroadcastProgramUrlStore.beenSent(crid, programUrl, serviceIdRef));
        
        sentBroadcastProgramUrlStore.removeSentRecord(crid, programUrl, serviceIdRef);
        assertFalse(sentBroadcastProgramUrlStore.beenSent(crid, programUrl, serviceIdRef));
    }
}
