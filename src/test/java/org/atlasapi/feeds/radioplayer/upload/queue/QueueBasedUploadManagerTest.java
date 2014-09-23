package org.atlasapi.feeds.radioplayer.upload.queue;

import static org.atlasapi.feeds.radioplayer.upload.queue.QueueBasedUploadManager.ATTEMPT_ID_KEY;
import static org.atlasapi.feeds.radioplayer.upload.queue.QueueBasedUploadManager.UPLOAD_TIME_KEY;
import static org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt.failedRemoteCheck;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;

import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.FileHistoryStore;
import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;
import com.mongodb.MongoException;


public class QueueBasedUploadManagerTest {
    
    private Clock clock = new TimeMachine(DateTime.now(DateTimeZone.UTC));
    @SuppressWarnings("unchecked")
    private TaskQueue<UploadTask> uploadQueue = Mockito.mock(TaskQueue.class);
    @SuppressWarnings("unchecked")
    private TaskQueue<RemoteCheckTask> remoteCheckQueue = Mockito.mock(TaskQueue.class);
    private FileHistoryStore fileStore = Mockito.mock(FileHistoryStore.class);
    private final UploadManager manager = new QueueBasedUploadManager(uploadQueue, remoteCheckQueue, fileStore);

    @Test
    public void testEnqueueOnAlreadyUploadEnqueuedTaskDoesNothing() {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        
        Mockito.when(uploadQueue.contains(file)).thenReturn(true);
        
        manager.enqueueUploadTask(task);
        
        Mockito.verify(uploadQueue, never()).push(task);
    }

    @Test
    public void testEnqueueOnAlreadyRemoteCheckEnqueuedTaskDoesNothing() {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        
        Mockito.when(remoteCheckQueue.contains(file)).thenReturn(true);
        
        manager.enqueueUploadTask(task);
        
        Mockito.verify(uploadQueue, never()).push(task);
    }
    
    @Test
    public void testUploadsToQueueIfNotAlreadyEnqueued() {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        FileHistory fileHistory = new FileHistory(file);
        Mockito.when(uploadQueue.contains(file)).thenReturn(false);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(fileHistory));
        
        manager.enqueueUploadTask(task);
        
        Mockito.verify(uploadQueue).push(task);
    }
    
    @Test
    public void testIfNoFileHistoryCreationAndStorageTakesPlace() {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.<FileHistory>absent());
        
        manager.enqueueUploadTask(task);
        
        ArgumentCaptor<FileHistory> storedHistory = ArgumentCaptor.forClass(FileHistory.class);
        
        Mockito.verify(fileStore).store(storedHistory.capture());
        Mockito.verify(uploadQueue).push(task);
        
        assertEquals(new FileHistory(file), storedHistory.getValue());
    }
    
    @Test(expected = MongoException.class)
    public void testExceptionsPropagatedUp() {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        MongoException something = new MongoException("Mongo no likey");
        Mockito.when(fileStore.fetch(file)).thenThrow(something);
        
        manager.enqueueUploadTask(task);
    }
    
    @Test(expected = InvalidStateException.class)
    public void testNoRecordWhenUpdatingThrows() throws InvalidStateException {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        UploadAttempt result = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.<String,String>of());
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.<FileHistory>absent());
        
        manager.recordUploadResult(task, result);
    }
    
    @Test
    public void testSuccessfulUploadUpdatesQueuesAndRecord() throws InvalidStateException {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        UploadAttempt result = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.<String,String>of());
        UploadAttempt resultWithId = result.copyWithId(1234l);
        RemoteCheckTask remoteCheckTask = createRemoteCheckTask(task, resultWithId);
        
        FileHistory history = new FileHistory(file);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        Mockito.when(fileStore.addUploadAttempt(file, result)).thenReturn(resultWithId);
        
        manager.recordUploadResult(task, result);
        
        Mockito.verify(fileStore).addUploadAttempt(file, result);
        Mockito.verify(uploadQueue).remove(task);
        Mockito.verify(remoteCheckQueue).push(remoteCheckTask);
    }
    
    @Test
    public void testFailedUploadIsRetriedAndRecordUpdated() throws InvalidStateException {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        UploadAttempt result = UploadAttempt.failedUpload(clock.now(), ImmutableMap.<String,String>of());
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(new FileHistory(file)));
        
        manager.recordUploadResult(task, result);
        
        Mockito.verify(fileStore).addUploadAttempt(file, result);
        Mockito.verify(uploadQueue).remove(task);
        Mockito.verifyZeroInteractions(remoteCheckQueue);
    }
    
    @Test
    public void testUnknownUploadIsRetriedAndRecordUpdated() throws InvalidStateException {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        // unlikely, given that you have to manually construct it using the UNKNOWN result type
        UploadAttempt result = new UploadAttempt(clock.now(), FileUploadResultType.UNKNOWN, ImmutableMap.<String,String>of(), null, null);
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(new FileHistory(file)));
        
        manager.recordUploadResult(task, result);
        
        Mockito.verify(fileStore).addUploadAttempt(file, result);
        Mockito.verify(uploadQueue).push(task);
        Mockito.verifyZeroInteractions(remoteCheckQueue);
    }

    @Test
    public void testSuccessfulRemoteCheckUpdatesQueuesAndRecord() throws InvalidStateException {
        long id = 12345667l;
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        RemoteCheckTask task = new RemoteCheckTask(file, ImmutableMap.of(ATTEMPT_ID_KEY, String.valueOf(id)));
        RemoteCheckResult result = RemoteCheckResult.success("Successed");
        
        FileHistory history = new FileHistory(file);
        UploadAttempt upload = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.<String, String>of());
        upload = upload.copyWithId(id);
        history.addUploadAttempt(upload);
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.recordRemoteCheckResult(task, result);
        
        
        FileHistory updatedFile = FileHistory.copyWithAttempts(history, ImmutableList.of(UploadAttempt.successfulRemoteCheck(upload, result.message())));
        ArgumentCaptor<FileHistory> storedFile = ArgumentCaptor.forClass(FileHistory.class);
        Mockito.verify(fileStore).store(storedFile.capture());
        Mockito.verify(remoteCheckQueue).remove(task);
        
        assertEquals(updatedFile, storedFile.getValue());
        assertEquals(updatedFile.uploadAttempts(), storedFile.getValue().uploadAttempts());
    }

    @Test(expected = InvalidStateException.class)
    public void testNoRecordWhenRecordingRemoteCheckThrows() throws InvalidStateException {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        RemoteCheckTask task = new RemoteCheckTask(file, ImmutableMap.<String, String>of());
        RemoteCheckResult result = RemoteCheckResult.success("Successed");
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.<FileHistory>absent());
        
        manager.recordRemoteCheckResult(task, result);
    }
    
    @Test
    public void testFailedRemoteCheckReenqueuesAndRecords() throws InvalidStateException {
        long id = 12345667l;
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        RemoteCheckTask task = new RemoteCheckTask(file, ImmutableMap.of(ATTEMPT_ID_KEY, String.valueOf(id)));
        RemoteCheckResult result = RemoteCheckResult.failure("Doh!");
        
        FileHistory history = new FileHistory(file);
        UploadAttempt upload = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.<String, String>of());
        upload = upload.copyWithId(id);
        history.addUploadAttempt(upload);
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.recordRemoteCheckResult(task, result);
        
        Mockito.verify(remoteCheckQueue).remove(task);
        
        FileHistory updatedFile = FileHistory.copyWithAttempts(history, ImmutableList.of(failedRemoteCheck(upload, result.message())));
        
        ArgumentCaptor<FileHistory> storedFile = ArgumentCaptor.forClass(FileHistory.class);
        
        Mockito.verify(fileStore).store(storedFile.capture());
        
        FileHistory captured = storedFile.getValue();
        assertEquals(updatedFile, captured);
        assertEquals(updatedFile.uploadAttempts(), captured.uploadAttempts());
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(updatedFile));
        
        UploadTask uploadTask = new UploadTask(file);
        Mockito.verify(uploadQueue).push(uploadTask);
    }

    @Test
    public void testUnknownRemoteCheckReenqueuesAndRecords() throws InvalidStateException {
        long id = 12345667l;
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        RemoteCheckTask task = new RemoteCheckTask(file, ImmutableMap.of(ATTEMPT_ID_KEY, String.valueOf(id)));
        RemoteCheckResult result = RemoteCheckResult.unknown("Erm...");
        
        FileHistory history = new FileHistory(file);
        UploadAttempt upload = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.<String, String>of());
        upload = upload.copyWithId(id);
        history.addUploadAttempt(upload);
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.recordRemoteCheckResult(task, result);
        
        Mockito.verify(remoteCheckQueue).push(task);
        
        FileHistory updatedFile = FileHistory.copyWithAttempts(history, ImmutableList.of(
                new UploadAttempt(upload.id(), upload.uploadTime(), upload.uploadResult(), upload.uploadDetails(), result.result(), result.message())
        ));
        
        ArgumentCaptor<FileHistory> storedFile = ArgumentCaptor.forClass(FileHistory.class);
        
        Mockito.verify(fileStore).store(storedFile.capture());
        
        assertEquals(updatedFile, storedFile.getValue());
        assertEquals(updatedFile.uploadAttempts(), storedFile.getValue().uploadAttempts());
    }
    
    private RemoteCheckTask createRemoteCheckTask(UploadTask task, UploadAttempt result) {
        return new RemoteCheckTask(task.file(), ImmutableMap.<String, String>builder()
                .putAll(result.uploadDetails())
                .put(UPLOAD_TIME_KEY, String.valueOf(result.uploadTime().getMillis()))
                .put(ATTEMPT_ID_KEY, String.valueOf(result.id()))
                .build());
    }
}
