package org.atlasapi.feeds.youview.revocation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class MongoRevokedContentStoreTest {

    private static final String A_CONTENT_URI = "some uri";
    private static final String A_DIFFERENT_URI = "another uri";
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private final RevokedContentStore store = new MongoRevokedContentStore(mongo);
    
    @After
    public void tearDown() {
        MongoTestHelper.clearDB();
    }
    
    @Test
    public void testCallingIsRevokedWhenNoRevokedContentReturnsFalse() {
        assertFalse("Calling isRevoked when no Content is revoked should return false", store.isRevoked(A_CONTENT_URI));
    }

    @Test
    public void testCallingIsRevokedAfterRevokingContentReturnsTrue() {
        store.revoke(A_CONTENT_URI);
        
        assertTrue("Calling isRevoked when Content is revoked should return true", store.isRevoked(A_CONTENT_URI));
    }
    
    @Test
    public void testCallingIsRevokedAfterRevokingDifferentContentReturnsFalse() {
        store.revoke(A_CONTENT_URI);
        
        assertFalse("Calling isRevoked when different Content is revoked should return false", store.isRevoked(A_DIFFERENT_URI));
    }
    
    @Test
    public void testCallingIsRevokedAfterRevocationAndUnrevocationReturnsFalse() {
        store.revoke(A_CONTENT_URI);
        boolean unrevoked = store.unrevoke(A_CONTENT_URI);
        
        assertFalse("revoking and unrevoking Content should cause isRevoked to return false", store.isRevoked(A_CONTENT_URI));
        assertTrue("unrevoke call should return true if content is revoked", unrevoked);
    }
    
    @Test
    public void testCallingUnrevocationOnNonRevokedContentReturnsFalse() {
        boolean unrevoked = store.unrevoke(A_CONTENT_URI);

        assertFalse("unrevoke call should return true if content is not revoked", unrevoked);
    }
}
