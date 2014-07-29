package org.atlasapi.feeds.radioplayer.upload.queue;

import static org.atlasapi.feeds.radioplayer.upload.persistence.UploadTaskTranslatorTest.createUploadTask;
import static org.atlasapi.feeds.radioplayer.upload.queue.UploadQueueWorker.ERROR_KEY;
import static org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploader.FILENAME_KEY;
import static org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploader.HASHCODE_KEY;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class UploadQueueWorkerTest {

    @SuppressWarnings("unchecked")
    private TaskQueue<UploadTask> uploadQueue = Mockito.mock(TaskQueue.class);
    private final FileUploader uploader = Mockito.mock(FileUploader.class);
    private UploadServicesSupplier uploaderSupplier = Mockito.mock(UploadServicesSupplier.class);
    private Clock clock = new TimeMachine(DateTime.now());
    private FileCreator fileCreator = Mockito.mock(FileCreator.class);
    private InteractionManager stateUpdater = Mockito.mock(InteractionManager.class);
    private final UploadQueueWorker queueWorker = new UploadQueueWorker(uploadQueue, uploaderSupplier, clock, fileCreator, stateUpdater);
    
    @Test
    public void testFileUploadsSuccessfully() throws FileUploadException, IOException, InvalidStateException {
        UploadTask task = createUploadTask(UploadService.S3);
        
        FileUpload upload = new FileUpload("someupload", new byte[0]);
        Mockito.when(fileCreator.createFile(task.service(), task.type(), task.date())).thenReturn(upload);
        
        Mockito.when(uploaderSupplier.get(Mockito.any(UploadService.class), Mockito.any(DateTime.class), Mockito.any(FileType.class))).thenReturn(Optional.of(uploader));
        
        UploadAttempt successfulResult = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.of(HASHCODE_KEY, "ab402f340dac", FILENAME_KEY, upload.getFilename()));
        Mockito.when(uploader.upload(upload)).thenReturn(successfulResult);
        
        queueWorker.processTask(task);
        
        Mockito.verify(stateUpdater).recordUploadResult(task, successfulResult);
    }
    
    @Test
    public void testUploaderExceptionRecordsFailure() throws FileUploadException, IOException, InvalidStateException {
        UploadTask task = createUploadTask(UploadService.S3);
        
        FileUpload upload = new FileUpload("someupload", new byte[0]);
        Mockito.when(fileCreator.createFile(task.service(), task.type(), task.date())).thenReturn(upload);
        
        Mockito.when(uploaderSupplier.get(Mockito.any(UploadService.class), Mockito.any(DateTime.class), Mockito.any(FileType.class))).thenReturn(Optional.of(uploader));
        
        FileUploadException uploadException = new FileUploadException("An error occurred");
        Mockito.when(uploader.upload(upload)).thenThrow(uploadException);
        
        queueWorker.processTask(task);
        
        ArgumentCaptor<UploadAttempt> result = ArgumentCaptor.forClass(UploadAttempt.class);
        ArgumentCaptor<UploadTask> taskCaptor = ArgumentCaptor.forClass(UploadTask.class);
        Mockito.verify(stateUpdater).recordUploadResult(taskCaptor.capture(), result.capture());
        
        assertEquals(task, taskCaptor.getValue());
        
        assertEquals(FileUploadResultType.FAILURE, result.getValue().uploadResult());
        assertEquals(clock.now(), result.getValue().uploadTime());
        assertEquals(String.valueOf(uploadException), result.getValue().uploadDetails().get(ERROR_KEY));
    }
    
    @Test
    public void testFileCreationFailureRecordsFailure() throws FileUploadException, IOException, InvalidStateException {
        UploadTask task = createUploadTask(UploadService.S3);
        
        IOException fileException = new IOException("file creation failed");
        Mockito.when(fileCreator.createFile(task.service(), task.type(), task.date())).thenThrow(fileException);
        
        queueWorker.processTask(task);

        ArgumentCaptor<UploadAttempt> result = ArgumentCaptor.forClass(UploadAttempt.class);
        ArgumentCaptor<UploadTask> taskCaptor = ArgumentCaptor.forClass(UploadTask.class);
        Mockito.verify(stateUpdater).recordUploadResult(taskCaptor.capture(), result.capture());
        
        assertEquals(task, taskCaptor.getValue());
        
        assertEquals(FileUploadResultType.FAILURE, result.getValue().uploadResult());
        assertEquals(clock.now(), result.getValue().uploadTime());
        assertEquals(String.valueOf(fileException), result.getValue().uploadDetails().get(ERROR_KEY));
    }
    
    @Test
    public void testFailureRecordedIfNoUploaderFound() throws FileUploadException, IOException, InvalidStateException {
        UploadTask task = createUploadTask(UploadService.S3);
        
        FileUpload upload = new FileUpload("someupload", new byte[0]);
        Mockito.when(fileCreator.createFile(task.service(), task.type(), task.date())).thenReturn(upload);
        
        Mockito.when(uploaderSupplier.get(Mockito.any(UploadService.class), Mockito.any(DateTime.class), Mockito.any(FileType.class))).thenReturn(Optional.<FileUploader>absent());
        
        queueWorker.processTask(task);

        ArgumentCaptor<UploadAttempt> result = ArgumentCaptor.forClass(UploadAttempt.class);
        ArgumentCaptor<UploadTask> taskCaptor = ArgumentCaptor.forClass(UploadTask.class);
        Mockito.verify(stateUpdater).recordUploadResult(taskCaptor.capture(), result.capture());
        
        assertEquals(task, taskCaptor.getValue());
        
        assertEquals(FileUploadResultType.FAILURE, result.getValue().uploadResult());
        assertEquals(clock.now(), result.getValue().uploadTime());
    }
}
