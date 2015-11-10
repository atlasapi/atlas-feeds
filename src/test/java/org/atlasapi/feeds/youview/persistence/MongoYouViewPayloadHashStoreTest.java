package org.atlasapi.feeds.youview.persistence;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;

public class MongoYouViewPayloadHashStoreTest {

    private YouViewPayloadHashStore store;

    @Before
    public void setUp() throws Exception {
        DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
        store = new MongoYouViewPayloadHashStore(mongo);
    }

    @Test
    public void testSaveAndRetrievePayloadHash() throws Exception {
        HashType payloadType = HashType.CONTENT;
        String imi = "imi";
        String hash = "hash";

        store.saveHash(payloadType, imi, hash);

        Optional<String> storedHash = store.getHash(payloadType, imi);

        assertThat(storedHash.isPresent(), is(true));
        assertThat(storedHash.get(), is(hash));
    }

    @Test
    public void missingHashReturnsOptionalAbsent() {
        Optional<String> storedHash = store.getHash(HashType.ON_DEMAND, "derp derp");
        assertThat(storedHash.isPresent(), is(false));
    }
}
