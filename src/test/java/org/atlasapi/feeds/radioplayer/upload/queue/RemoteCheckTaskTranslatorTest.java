package org.atlasapi.feeds.radioplayer.upload.queue;

import static org.atlasapi.feeds.radioplayer.upload.queue.QueueBasedUploadManager.UPLOAD_TIME_KEY;
import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.RemoteCheckTaskTranslator;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class RemoteCheckTaskTranslatorTest {

    private DBCollection mongo = MongoTestHelper.anEmptyTestDatabase().collection("testCollection");
    
    private final RemoteCheckTaskTranslator translator = new RemoteCheckTaskTranslator();
    
    @Test
    public void testTranslationOfTask() {
        RemoteCheckTask task = createRemoteCheckTask();
        
        RemoteCheckTask translated = translateViaMongo(task);
        
        assertEquals(task.type(), translated.type());
        assertEquals(task.service(), translated.service());
        assertEquals(task.uploadService(), translated.uploadService());
        assertEquals(task.date(), translated.date());
        assertEquals(task.uploadDetails(), translated.uploadDetails());
    }

    static RemoteCheckTask createRemoteCheckTask() {
        return new RemoteCheckTask(
                new RadioPlayerFile(
                    UploadService.S3, 
                    Iterables.getFirst(RadioPlayerServices.services, null), 
                    FileType.PI,
                    new LocalDate()
                ), 
                ImmutableMap.of("key", "value", UPLOAD_TIME_KEY, String.valueOf(DateTime.now().getMillis())) 
        );
    }
    
    // This is required because when written/read from Mongo, the embedded Map in the DBObject
    // is converted into a nested DBObject. However, when simply translated to a DBObject and back,
    // the Map is preserved as a Map within the DBObject.
    private RemoteCheckTask translateViaMongo(RemoteCheckTask task) {
        DBObject translated = translator.toDBObject(task);
        mongo.save(translated);
        return translator.fromDBObject(mongo.findOne());
    }
}
