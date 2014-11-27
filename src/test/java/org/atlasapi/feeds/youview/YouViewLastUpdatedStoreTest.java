package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.atlasapi.feeds.youview.persistence.MongoYouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class YouViewLastUpdatedStoreTest {

    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final Publisher ANOTHER_PUBLISHER = Publisher.LOVEFILM;
    private static final DateTime DATE_TIME = DateTime.now();
    
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private final YouViewLastUpdatedStore store = new MongoYouViewLastUpdatedStore(mongo);
    
    @Before
    public void setup() {
        MongoTestHelper.clearDB();
    }
    
    @Test
    public void testNoRecordForPublisherWithoutWrite() {
        Optional<DateTime> lastUpdated = store.getLastUpdated(PUBLISHER);
        
        assertFalse("No record should be present in store if last-updated time not written for publisher", lastUpdated.isPresent());
    }
    
    @Test
    public void testWritingRecordForPublisherEnsuresRecordIsReturnedForThatPublisher() {
        store.setLastUpdated(DATE_TIME, PUBLISHER);
        
        assertEquals(DATE_TIME.getMillis(), store.getLastUpdated(PUBLISHER).get().getMillis());
    }
    
    @Test
    public void testWritingRecordForPublisherEnsuresNoRecordIsReturnedForADifferentPublisher() {
        store.setLastUpdated(DATE_TIME, PUBLISHER);
        
        Optional<DateTime> lastUpdated = store.getLastUpdated(ANOTHER_PUBLISHER);
        assertFalse("No record should be present in store if last-updated time not written for publisher", lastUpdated.isPresent());
    }
}
