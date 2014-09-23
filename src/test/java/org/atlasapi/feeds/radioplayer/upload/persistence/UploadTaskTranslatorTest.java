package org.atlasapi.feeds.radioplayer.upload.persistence;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.UploadTaskTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadTask;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.collect.Iterables;


public class UploadTaskTranslatorTest {

    private final UploadTaskTranslator translator = new UploadTaskTranslator();
    
    @Test
    public void testTranslationOfTask() {
        UploadTask task = createUploadTask(UploadService.S3);
        
        UploadTask translated = translator.fromDBObject(translator.toDBObject(task));
        
        assertEquals(task.type(), translated.type());
        assertEquals(task.service(), translated.service());
        assertEquals(task.uploadService(), translated.uploadService());
        assertEquals(task.date(), translated.date());
    }

    public static UploadTask createUploadTask(UploadService uploadService) {
        return new UploadTask(new RadioPlayerFile(
                uploadService, 
                Iterables.getFirst(RadioPlayerServices.services, null), 
                FileType.PI,
                new LocalDate()
        ));
    }
}
