package org.atlasapi.feeds.radioplayer.upload.queue;

import static org.atlasapi.feeds.radioplayer.upload.queue.QueueBasedInteractionManager.ATTEMPT_ID_KEY;
import static org.atlasapi.feeds.radioplayer.upload.queue.QueueBasedInteractionManager.UPLOAD_TIME_KEY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;

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


public class QueueBasedInteractionManagerTest {
    
    private Clock clock = new TimeMachine(DateTime.now(DateTimeZone.UTC));
    @SuppressWarnings("unchecked")
    private TaskQueue<UploadTask> uploadQueue = Mockito.mock(TaskQueue.class);
    @SuppressWarnings("unchecked")
    private TaskQueue<RemoteCheckTask> remoteCheckQueue = Mockito.mock(TaskQueue.class);
    private FileHistoryStore fileStore = Mockito.mock(FileHistoryStore.class);
    private final InteractionManager manager = new QueueBasedInteractionManager(uploadQueue, remoteCheckQueue, fileStore);

    @Test
    public void testEnqueueOnAlreadyEnqueuedTaskDoesNothing() {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        
        FileHistory history = createFileRecord(file, true, false);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.enqueueUploadTask(task);
        
        Mockito.verifyZeroInteractions(uploadQueue);
        Mockito.verify(fileStore, never()).store(Mockito.any(FileHistory.class));
        
        history = createFileRecord(file, false, true);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.enqueueUploadTask(task);
        
        Mockito.verifyZeroInteractions(uploadQueue);
        Mockito.verify(fileStore, never()).store(Mockito.any(FileHistory.class));
    }
    
    @Test
    public void testRetrievesExistingRecordUpdatesAndUploadsToQueue() {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        
        FileHistory history = createFileRecord(file, false, false);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.enqueueUploadTask(task);
        
        Mockito.verify(fileStore).fetch(file);
        
        ArgumentCaptor<FileHistory> storedFile = ArgumentCaptor.forClass(FileHistory.class);
        
        Mockito.verify(fileStore).store(storedFile.capture());
        Mockito.verify(uploadQueue).push(task);
        
        FileHistory createdFile = FileHistory.copyWithAttempts(history, ImmutableList.<UploadAttempt>of());
        createdFile.setEnqueuedForUpload(true);
        
        assertEquals(createdFile, storedFile.getValue());
    }
    
    @Test
    public void testIfNoFileHistoryCreationAndStorageTakesPlace() {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.<FileHistory>absent());
        
        manager.enqueueUploadTask(task);
        
        FileHistory createdHistory = createFileRecord(file, true, false);
        
        ArgumentCaptor<FileHistory> storedHistory = ArgumentCaptor.forClass(FileHistory.class);
        
        Mockito.verify(fileStore).store(storedHistory.capture());
        Mockito.verify(uploadQueue).push(task);
        
        assertEquals(createdHistory, storedHistory.getValue());
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
        
        FileHistory history = createFileRecord(file, true, false);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        result = result.copyWithId(1234l);
        Mockito.when(fileStore.addUploadAttempt(file, result)).thenReturn(result);
        
        manager.recordUploadResult(task, result);
        
        Mockito.verify(uploadQueue).remove(task);
        
        RemoteCheckTask remoteCheckTask = createRemoteCheckTask(task, result);
        Mockito.verify(remoteCheckQueue).push(remoteCheckTask);
        
        FileHistory updatedFile = FileHistory.copyWithAttempts(history, ImmutableList.of(result));
        updatedFile.setEnqueuedForUpload(false);
        updatedFile.setEnqueuedForRemoteCheck(true);
        
        Mockito.verify(fileStore).successfulUpload(file);
    }
    
    @Test
    public void testFailedUploadIsRetriedAndRecordUpdated() throws InvalidStateException {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        UploadAttempt result = UploadAttempt.failedUpload(clock.now(), ImmutableMap.<String,String>of());
        
        FileHistory history = createFileRecord(file, true, false);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.recordUploadResult(task, result);
        
        Mockito.verify(uploadQueue).push(task);
        Mockito.verifyZeroInteractions(remoteCheckQueue);
    }
    
    @Test
    public void testUnknownUploadIsRetriedAndRecordUpdated() throws InvalidStateException {
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        UploadTask task = new UploadTask(file);
        // unlikely, given that you have to manually construct it using the UNKNOWN result type
        UploadAttempt result = new UploadAttempt(clock.now(), FileUploadResultType.UNKNOWN, ImmutableMap.<String,String>of(), null, null);
        
        FileHistory history = createFileRecord(file, true, false);
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        long attemptId = 1234l;
        Mockito.when(fileStore.addUploadAttempt(file, result)).thenReturn(result.copyWithId(attemptId));
        
        manager.recordUploadResult(task, result);
        
        Mockito.verify(uploadQueue).push(task);
        Mockito.verifyZeroInteractions(remoteCheckQueue);
    }

    // recordRemoteCheckResult
    // test successful remote check updates flags, attempt record, and dequeues
    @Test
    public void testSuccessfulRemoteCheckUpdatesQueuesAndRecord() throws InvalidStateException {
        long id = 12345667l;
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        RemoteCheckTask task = new RemoteCheckTask(file, ImmutableMap.of(ATTEMPT_ID_KEY, String.valueOf(id)));
        RemoteCheckResult result = RemoteCheckResult.success("Successed");
        
        FileHistory history = createFileRecord(file, false, true);
        UploadAttempt upload = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.<String, String>of());
        upload = upload.copyWithId(id);
        history.addUploadAttempt(upload);
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.recordRemoteCheckResult(task, result);
        
        Mockito.verify(remoteCheckQueue).remove(task);
        
        FileHistory updatedFile = FileHistory.copyWithAttempts(history, ImmutableList.of(updateAttempt(upload, result)));
        updatedFile.setEnqueuedForRemoteCheck(false);
        
        ArgumentCaptor<FileHistory> storedFile = ArgumentCaptor.forClass(FileHistory.class);
        
        Mockito.verify(fileStore).store(storedFile.capture());
        
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
        
        FileHistory history = createFileRecord(file, false, true);
        UploadAttempt upload = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.<String, String>of());
        upload = upload.copyWithId(id);
        history.addUploadAttempt(upload);
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.recordRemoteCheckResult(task, result);
        
        Mockito.verify(remoteCheckQueue).remove(task);
        
        FileHistory updatedFile = FileHistory.copyWithAttempts(history, ImmutableList.of(updateAttempt(upload, result)));
        updatedFile.setEnqueuedForRemoteCheck(false);
        
        ArgumentCaptor<FileHistory> storedFile = ArgumentCaptor.forClass(FileHistory.class);
        
        Mockito.verify(fileStore, times(2)).store(storedFile.capture());
        
        List<FileHistory> captures = storedFile.getAllValues();
        assertEquals(updatedFile, captures.get(0));
        assertEquals(updatedFile.uploadAttempts(), captures.get(0).uploadAttempts());
        
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(updatedFile));
        
        UploadTask uploadTask = new UploadTask(file);
        Mockito.verify(uploadQueue).push(uploadTask);
        
        FileHistory afterUploadFile = FileHistory.copyWithAttempts(updatedFile, updatedFile.uploadAttempts());
        afterUploadFile.setEnqueuedForUpload(true);
        
        assertEquals(afterUploadFile, captures.get(1));
    }

    @Test
    public void testUnknownRemoteCheckReenqueuesAndRecords() throws InvalidStateException {
        long id = 12345667l;
        RadioPlayerFile file = new RadioPlayerFile(UploadService.S3, Iterables.getFirst(RadioPlayerServices.services, null), FileType.PI, new LocalDate());
        RemoteCheckTask task = new RemoteCheckTask(file, ImmutableMap.of(ATTEMPT_ID_KEY, String.valueOf(id)));
        RemoteCheckResult result = RemoteCheckResult.unknown("Erm...");
        
        FileHistory history = createFileRecord(file, false, true);
        UploadAttempt upload = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.<String, String>of());
        upload = upload.copyWithId(id);
        history.addUploadAttempt(upload);
        
        Mockito.when(fileStore.fetch(file)).thenReturn(Optional.of(history));
        
        manager.recordRemoteCheckResult(task, result);
        
        Mockito.verify(remoteCheckQueue).push(task);
        
        FileHistory updatedFile = FileHistory.copyWithAttempts(history, ImmutableList.of(updateAttempt(upload, result)));
        
        ArgumentCaptor<FileHistory> storedFile = ArgumentCaptor.forClass(FileHistory.class);
        
        Mockito.verify(fileStore).store(storedFile.capture());
        
        assertEquals(updatedFile, storedFile.getValue());
        assertEquals(updatedFile.uploadAttempts(), storedFile.getValue().uploadAttempts());
    }
    
    private UploadAttempt updateAttempt(UploadAttempt upload, RemoteCheckResult result) {
        switch(result.result()) {
        case FAILURE:
            return UploadAttempt.failedRemoteCheck(upload, result.message());
        case SUCCESS:
            return UploadAttempt.successfulRemoteCheck(upload);
        case UNKNOWN:
        default:
            return new UploadAttempt(upload.id(), upload.uploadTime(), upload.uploadResult(), upload.uploadDetails(), 
                    result.result(), result.message());
        }
    }
    
    private RemoteCheckTask createRemoteCheckTask(UploadTask task, UploadAttempt result) {
        return new RemoteCheckTask(task.file(), ImmutableMap.<String, String>builder()
                .putAll(result.uploadDetails())
                .put(UPLOAD_TIME_KEY, String.valueOf(result.uploadTime().getMillis()))
                .put(ATTEMPT_ID_KEY, String.valueOf(result.id()))
                .build());
    }
    
    private FileHistory createFileRecord(RadioPlayerFile file, boolean enqueuedForUpload, boolean enqueuedForRemoteCheck) {
        FileHistory history = new FileHistory(file);
        history.setEnqueuedForUpload(enqueuedForUpload);
        history.setEnqueuedForRemoteCheck(enqueuedForRemoteCheck);
        return history;
    }

}
