package org.atlasapi.feeds.radioplayer.upload.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.MongoTaskQueue;
import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.atlasapi.feeds.radioplayer.upload.persistence.UploadTaskTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.MongoTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadTask;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class MongoTaskQueueTest {

    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private MongoTranslator<UploadTask> translator = new UploadTaskTranslator();
    private Clock clock = new TimeMachine(DateTime.now(DateTimeZone.UTC));
    private final TaskQueue<UploadTask> queue = new MongoTaskQueue<UploadTask>(mongo, "testQueue", translator, clock );
    
    @Test
    public void testFetchOnEmptyQueueReturnsAbsent() {
        Optional<UploadTask> fetched = queue.fetchOne();
        
        assertFalse(fetched.isPresent());
    }
    
    @Test
    public void testPushAndFetch() {
        UploadTask task = createUploadTask(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10));
        
        queue.push(task);
        
        Optional<UploadTask> fetched = queue.fetchOne();
        
        assertEquals(task, fetched.get());
    }

    @Test
    public void testMultiFetchWithoutRemoveReturnsSameItem() {
        UploadTask task = createUploadTask(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10));
        
        queue.push(task);
        
        Optional<UploadTask> fetched = queue.fetchOne();
        
        assertEquals(task, fetched.get());
        
        fetched = queue.fetchOne();
        
        assertEquals(task, fetched.get());
    }
    
    @Test
    public void testRemove() {
        UploadTask task = createUploadTask(UploadService.HTTPS, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10));
        
        queue.push(task);
        Optional<UploadTask> fetched = queue.fetchOne();
        assertTrue(fetched.isPresent());
        
        assertTrue(queue.remove(fetched.get()));
        
        fetched = queue.fetchOne();
        assertFalse(fetched.isPresent());
    }
    
    @Test
    public void testRemoveOnEmptyQueueReturnsFalse() {
        UploadTask task = createUploadTask(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10));
        
        assertFalse(queue.remove(task));
    }
    
    @Test
    public void testRemoveOfAbsentTaskReturnsFalse() {
        UploadTask enqueued = createUploadTask(UploadService.HTTPS, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10));
        
        UploadTask notEnqueued = createUploadTask(UploadService.S3, Iterables.get(RadioPlayerServices.services, 5), FileType.OD, new LocalDate(2014, 03, 10));
        
        queue.push(enqueued);
        
        assertFalse(queue.remove(notEnqueued));
        assertTrue(queue.remove(enqueued));
    }
    
    @Test
    public void testFetchReturnsOldestTimestampedTask() {
        UploadTask somethingOld = createUploadTask(UploadService.HTTPS, Iterables.getFirst(RadioPlayerServices.services, null), FileType.OD, new LocalDate(2014, 07, 10), clock.now().minusDays(10));
        UploadTask somethingNew = createUploadTask(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10), clock.now());
        
        queue.push(somethingOld);
        queue.push(somethingNew);
        
        assertEquals(somethingOld, queue.fetchOne().get());
    }
    
    @Test
    public void testPushOverwritesIfSameButNewer() {
        UploadTask somethingOld = createUploadTask(UploadService.HTTPS, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10), DateTime.now(DateTimeZone.UTC).minusDays(10));
        UploadTask somethingNew = createUploadTask(UploadService.HTTPS, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10), DateTime.now(DateTimeZone.UTC));
        
        queue.push(somethingOld);
        queue.push(somethingNew);
        
        assertEquals(somethingNew, queue.fetchOne().get());
    }
    
    @Test
    public void testTimestampAddedIfNull() {
        UploadTask task = createUploadTask(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10));
        
        queue.push(task);
        
        Optional<UploadTask> fetched = queue.fetchOne();
        
        assertEquals(clock.now(), fetched.get().timestamp());
    }
    
    @Test
    public void testTimestampUnalteredIfNonNull() {
        UploadTask task = createUploadTask(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate(2014, 07, 10), clock.now().minusDays(10));
        
        queue.push(task);
        
        Optional<UploadTask> fetched = queue.fetchOne();
        
        assertEquals(task.timestamp(), fetched.get().timestamp());
    }
    
    private UploadTask createUploadTask(UploadService uploadService, RadioPlayerService service, FileType type,
            LocalDate date) {
        return new UploadTask(new RadioPlayerFile(uploadService, service, type, date));
    }
    
    private UploadTask createUploadTask(UploadService uploadService, RadioPlayerService service, FileType type,
            LocalDate date, DateTime timestamp) {
        return new UploadTask(new RadioPlayerFile(uploadService, service, type, date), timestamp);
    }
}

