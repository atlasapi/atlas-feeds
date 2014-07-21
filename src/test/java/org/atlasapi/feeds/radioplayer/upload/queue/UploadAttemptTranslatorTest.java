package org.atlasapi.feeds.radioplayer.upload.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.atlasapi.feeds.radioplayer.upload.persistence.UploadAttemptTranslator;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class UploadAttemptTranslatorTest {
    
    private DBCollection mongo = MongoTestHelper.anEmptyTestDatabase().collection("testCollection");
    
    private final UploadAttemptTranslator translator = new UploadAttemptTranslator();
    
    @Test
    public void testTranslationOfFullyPopulatedAttempt() {
        UploadAttempt attempt = createAttempt();
        
        UploadAttempt translated = translateViaMongo(attempt);
        
        assertEquals(attempt.uploadTime(), translated.uploadTime());
        assertEquals(attempt.uploadResult(), translated.uploadResult());
        assertEquals(attempt.uploadDetails(), translated.uploadDetails());
        assertEquals(attempt.remoteCheckResult(), translated.remoteCheckResult());
        assertEquals(attempt.remoteCheckMessage(), translated.remoteCheckMessage());
    }

    @Test
    public void testTranslationOfPartiallyPopulatedAttempt() {
        UploadAttempt attempt = createUnknownAttempt();
        
        UploadAttempt translated = translateViaMongo(attempt);
        
        assertNull(translated.uploadTime());
        assertEquals(attempt.uploadResult(), translated.uploadResult());
        assertEquals(attempt.uploadDetails(), translated.uploadDetails());
        assertNull(translated.remoteCheckResult());
        assertNull(translated.remoteCheckMessage());
    }

    private UploadAttempt createUnknownAttempt() {
        return UploadAttempt.enqueuedAttempt();
    }

    private UploadAttempt createAttempt() {
        return new UploadAttempt(
                DateTime.now(DateTimeZone.UTC), 
                FileUploadResultType.SUCCESS, 
                ImmutableMap.of("key", "value", "anotherKey", "a different value"), 
                FileUploadResultType.SUCCESS, 
                "success!"
        );
    }
    
    // This is required because when written/read from Mongo, the embedded Map in the DBObject
    // is converted into a nested DBObject. However, when simply translated to a DBObject and back,
    // the Map is preserved as a Map within the DBObject.
    private UploadAttempt translateViaMongo(UploadAttempt attempt) {
        DBObject translated = translator.toDBObject(attempt);
        mongo.save(translated);
        return translator.fromDBObject(mongo.findOne());
    }
}
