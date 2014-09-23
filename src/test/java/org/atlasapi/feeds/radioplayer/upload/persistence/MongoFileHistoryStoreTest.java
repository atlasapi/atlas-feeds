package org.atlasapi.feeds.radioplayer.upload.persistence;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.ids.IdGenerator;
import com.metabroadcast.common.ids.SequenceGenerator;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class MongoFileHistoryStoreTest {
    
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private IdGenerator generator = new SequenceGenerator();
    private final FileHistoryStore fileStore = new MongoFileHistoryStore(mongo, generator);

    @Test
    public void testStoreAndFetch() {
        FileHistory history = createFileHistory();
        
        fileStore.store(history);
        
        Optional<FileHistory> fetched = fileStore.fetch(history.file());
        
        assertEquals(fetched.get(), history);
    }

    @Test
    public void testAddUploadAttemptToFile() {
        FileHistory history = createFileHistory();
        fileStore.store(history);
        
        UploadAttempt attempt = UploadAttempt.successfulUpload(DateTime.now(), ImmutableMap.<String, String>of());
        
        UploadAttempt withId = fileStore.addUploadAttempt(history.file(), attempt);
        
        Optional<FileHistory> fetched = fileStore.fetch(history.file());
        
        assertEquals(withId.id(), Iterables.getOnlyElement(fetched.get().uploadAttempts()).id());
    }
    
    private FileHistory createFileHistory() {
        return new FileHistory(new RadioPlayerFile(
                UploadService.HTTPS, 
                Iterables.getFirst(RadioPlayerServices.services, null), 
                FileType.PI, 
                new LocalDate()
        ));
    }

}
