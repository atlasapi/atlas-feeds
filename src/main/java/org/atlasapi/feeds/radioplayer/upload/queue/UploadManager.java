package org.atlasapi.feeds.radioplayer.upload.queue;


public interface UploadManager {
    
    void enqueueUploadTask(UploadTask task);
    
    void recordUploadResult(UploadTask task, UploadAttempt result) throws InvalidStateException;
    
    void recordRemoteCheckResult(RemoteCheckTask task, RemoteCheckResult result) throws InvalidStateException;
}
