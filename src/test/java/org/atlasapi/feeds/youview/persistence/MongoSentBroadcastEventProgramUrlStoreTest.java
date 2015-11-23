package org.atlasapi.feeds.youview.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.atlasapi.feeds.youview.persistence.MongoSentBroadcastEventPcridStore;
import org.joda.time.LocalDate;
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
        String itemCrid = "crid1";
        String programmeCrid = "http://example.org/1";
        String broadcastEventImi = "imi:example.org/1234";
        LocalDate localDate = LocalDate.now();
        
        assertFalse(sentBroadcastProgramUrlStore.getSentBroadcastEventRecords(itemCrid, programmeCrid).isPresent());
            
        sentBroadcastProgramUrlStore.recordSent(broadcastEventImi, localDate, itemCrid, programmeCrid);
        assertEquals(broadcastEventImi, sentBroadcastProgramUrlStore.getSentBroadcastEventRecords(itemCrid, programmeCrid).get().getBroadcastEventImi());
        
        sentBroadcastProgramUrlStore.removeSentRecord(itemCrid, programmeCrid);
        assertFalse(sentBroadcastProgramUrlStore.getSentBroadcastEventRecords(itemCrid, programmeCrid).isPresent());
    }
}
