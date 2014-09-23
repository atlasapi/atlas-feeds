package org.atlasapi.feeds.radioplayer.upload.persistence;

import static org.junit.Assert.*;

import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.FileHistoryTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class FileHistoryTranslatorTest {

    private DBCollection mongo = MongoTestHelper.anEmptyTestDatabase().collection("testCollection");
    
    private final FileHistoryTranslator translator = new FileHistoryTranslator();
    
    @Test
    public void testFileHistoryTranslation() {
        FileHistory history = createFileHistory();
        
        FileHistory translated = translateViaMongo(history);
        
        assertEquals(history.file(), translated.file());
        for (UploadAttempt attempt : history.uploadAttempts()) {
            assertTrue(translated.uploadAttempts().contains(attempt));    
        }
    }
    
    private FileHistory createFileHistory() {
        return new FileHistory(
                new RadioPlayerFile(
                        UploadService.HTTPS, 
                        Iterables.getFirst(RadioPlayerServices.services, null), 
                        FileType.PI, 
                        new LocalDate(2014, 07, 10)
                ), 
                createUploadAttempts()
        );
    }

    private Iterable<UploadAttempt> createUploadAttempts() {
        UploadAttempt attempt = UploadAttempt.successfulUpload(DateTime.now(DateTimeZone.UTC), ImmutableMap.of("key", "value"));
        attempt = attempt.copyWithId(12345l);
        return ImmutableList.of(
                attempt
                );
    }
    // This is required because when written/read from Mongo, the embedded Map in the DBObject
    // is converted into a nested DBObject. However, when simply translated to a DBObject and back,
    // the Map is preserved as a Map within the DBObject.
    private FileHistory translateViaMongo(FileHistory file) {
        DBObject translated = translator.toDBObject(file);
        mongo.save(translated);
        return translator.fromDBObject(mongo.findOne());
    }
}
