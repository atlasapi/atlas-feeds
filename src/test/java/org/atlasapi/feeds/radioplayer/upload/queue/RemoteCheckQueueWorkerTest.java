package org.atlasapi.feeds.radioplayer.upload.queue;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.base.Optional;


public class RemoteCheckQueueWorkerTest {
    
    @SuppressWarnings("unchecked")
    private TaskQueue<RemoteCheckTask> remoteCheckQueue = Mockito.mock(TaskQueue.class);
    private final RemoteCheckService remoteChecker = Mockito.mock(RemoteCheckService.class);
    private RemoteCheckerSupplier checkerSupplier = Mockito.mock(RemoteCheckerSupplier.class);
    private InteractionManager stateUpdater = Mockito.mock(InteractionManager.class);
    private final QueueWorker<RemoteCheckTask> queueWorker = new RemoteCheckQueueWorker(remoteCheckQueue, checkerSupplier, stateUpdater); 

    // test successful check
    // test can't find checker
    // test checker throws exception
    
    @Test
    public void testChecksSuccessfully() throws RemoteCheckException, InvalidStateException {
        RemoteCheckTask task = RemoteCheckTaskTranslatorTest.createRemoteCheckTask();
        
        Mockito.when(checkerSupplier.get(Mockito.any(UploadService.class), Mockito.any(DateTime.class), Mockito.any(FileType.class))).thenReturn(Optional.of(remoteChecker));
        
        RemoteCheckResult result = RemoteCheckResult.success("I can haz success");
        Mockito.when(remoteChecker.check(task)).thenReturn(result);
        
        queueWorker.processTask(task);
        
        Mockito.verify(stateUpdater).recordRemoteCheckResult(task, result);
    }

    @Test
    public void testRemoteCheckFailure() throws RemoteCheckException, InvalidStateException {
        RemoteCheckTask task = RemoteCheckTaskTranslatorTest.createRemoteCheckTask();
        
        Mockito.when(checkerSupplier.get(Mockito.any(UploadService.class), Mockito.any(DateTime.class), Mockito.any(FileType.class))).thenReturn(Optional.of(remoteChecker));
        
        RemoteCheckResult result = RemoteCheckResult.failure("doh!");
        Mockito.when(remoteChecker.check(task)).thenReturn(result);
        
        queueWorker.processTask(task);

        Mockito.verify(stateUpdater).recordRemoteCheckResult(task, result);
    }

    @Test
    public void testLackOfRemoteCheckerRecordsFailure() throws RemoteCheckException, InvalidStateException {
        RemoteCheckTask task = RemoteCheckTaskTranslatorTest.createRemoteCheckTask();
        
        Mockito.when(checkerSupplier.get(Mockito.any(UploadService.class), Mockito.any(DateTime.class), Mockito.any(FileType.class))).thenReturn(Optional.<RemoteCheckService>absent());
        
        queueWorker.processTask(task);
        
        ArgumentCaptor<RemoteCheckResult> result = ArgumentCaptor.forClass(RemoteCheckResult.class);
        ArgumentCaptor<RemoteCheckTask> taskCaptor = ArgumentCaptor.forClass(RemoteCheckTask.class);
        Mockito.verify(stateUpdater).recordRemoteCheckResult(taskCaptor.capture(), result.capture());
        
        assertEquals(task, taskCaptor.getValue());
        
        assertEquals(FileUploadResultType.FAILURE, result.getValue().result());
    }

    @Test
    public void testRemoteCheckExceptionRecordsFailure() throws RemoteCheckException, InvalidStateException {
        RemoteCheckTask task = RemoteCheckTaskTranslatorTest.createRemoteCheckTask();
        
        Mockito.when(checkerSupplier.get(Mockito.any(UploadService.class), Mockito.any(DateTime.class), Mockito.any(FileType.class))).thenReturn(Optional.of(remoteChecker));
        
        RemoteCheckException exception = new RemoteCheckException("nopenopenope");
        Mockito.when(remoteChecker.check(task)).thenThrow(exception);
        
        queueWorker.processTask(task);
        
        ArgumentCaptor<RemoteCheckResult> result = ArgumentCaptor.forClass(RemoteCheckResult.class);
        ArgumentCaptor<RemoteCheckTask> taskCaptor = ArgumentCaptor.forClass(RemoteCheckTask.class);
        Mockito.verify(stateUpdater).recordRemoteCheckResult(taskCaptor.capture(), result.capture());
        
        assertEquals(task, taskCaptor.getValue());
        
        assertEquals(FileUploadResultType.FAILURE, result.getValue().result());
        assertEquals(String.valueOf(exception), result.getValue().message());
    }
}
