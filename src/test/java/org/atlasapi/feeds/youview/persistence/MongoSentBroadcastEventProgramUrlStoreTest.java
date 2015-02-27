package org.atlasapi.feeds.youview.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.atlasapi.feeds.youview.persistence.MongoSentBroadcastEventPcridStore;
import org.junit.Before;
import org.junit.Test;

import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class MongoSentBroadcastEventProgramUrlStoreTest {

    private DatabasedMongo mongo;
    private MongoSentBroadcastEventPcridStore sentBroadcastProgramUrlStore;
    
    @Before
    public void setUp() {
        mongo = MongoTestHelper.anEmptyTestDatabase();
        sentBroadcastProgramUrlStore = new MongoSentBroadcastEventPcridStore(mongo);
    }
    
    @Test
    public void testStoreAndRemove() {
        String crid = "crid1";
        String programUrl = "http://example.org/1";
        
        assertFalse(sentBroadcastProgramUrlStore.beenSent(crid, programUrl));
            
        sentBroadcastProgramUrlStore.recordSent(crid, programUrl);
        assertTrue(sentBroadcastProgramUrlStore.beenSent(crid, programUrl));
        
        sentBroadcastProgramUrlStore.removeSentRecord(crid, programUrl);
        assertFalse(sentBroadcastProgramUrlStore.beenSent(crid, programUrl));
    }
}
