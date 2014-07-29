package org.atlasapi.feeds.radioplayer.upload.persistence;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.UploadTaskTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.MongoTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadTask;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.collect.Iterables;


public class RadioPlayerUploadTaskTranslatorTest {

    private final MongoTranslator<UploadTask> translator = new UploadTaskTranslator();
    
    @Test
    public void testTranslation() {
        UploadTask task = new UploadTask(new RadioPlayerFile(UploadService.HTTPS, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10)));
        
        UploadTask translated = translator.fromDBObject(translator.toDBObject(task));
        
        assertEquals(task.type(), translated.type());
        assertEquals(task.uploadService(), translated.uploadService());
        assertEquals(task.service(), translated.service());
        assertEquals(task.date(), translated.date());
    }

}
