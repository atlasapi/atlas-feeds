package org.atlasapi.feeds.youview.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;

public class MongoIdMappingStoreTest {

    private DatabasedMongo mongo;
    private MongoIdMappingStore store;

    @Before
    public void setup() {
        mongo = MongoTestHelper.anEmptyTestDatabase();
        store = new MongoIdMappingStore(mongo);
    }

    @Test
    public void testStoreAndLoadIdMapping() {
        String key = "thisisthekey";
        String value = "thisisthevalue";

        store.storeMapping(key, value);
        Optional<String> retrievedValue = store.getValueFor(key);

        assertTrue(retrievedValue.isPresent());
        assertEquals(value, retrievedValue.get());
    }

    @Test
    public void testShouldReturnTheLatestWrittenValue() {
        String key = "thisisthekey";
        String value = "thisisthevalue";
        String newValue = "thisistheNewvalue";

        store.storeMapping(key, value);
        store.storeMapping(key, newValue);
        Optional<String> retrievedValue = store.getValueFor(key);

        assertTrue(retrievedValue.isPresent());
        assertEquals(newValue, retrievedValue.get());
    }

    @Test
    public void testReturnsOptionalForANotExistentKey() {
        String key = "thisisthenotexistentkey";

        Optional<String> retrievedValue = store.getValueFor(key);

        assertFalse(retrievedValue.isPresent());
    }


}