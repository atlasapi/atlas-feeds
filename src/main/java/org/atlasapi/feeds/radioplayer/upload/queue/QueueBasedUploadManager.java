package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.persistence.FileHistoryStore;
import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;


public class QueueBasedUploadManager implements UploadManager {
    
    static final String ATTEMPT_ID_KEY = "attemptId";
    static final String UPLOAD_TIME_KEY = "uploadTime";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TaskQueue<UploadTask> uploadQueue;
    private final TaskQueue<RemoteCheckTask> remoteCheckQueue;
    private final FileHistoryStore fileStore;
    
    public QueueBasedUploadManager(TaskQueue<UploadTask> uploadQueue, 
            TaskQueue<RemoteCheckTask> remoteCheckQueue, FileHistoryStore fileStore) {
        this.uploadQueue = checkNotNull(uploadQueue);
        this.remoteCheckQueue = checkNotNull(remoteCheckQueue);
        this.fileStore = checkNotNull(fileStore);
    }

    @Override
    public synchronized void enqueueUploadTask(UploadTask task) {
        // TODO see if this logic can be pushed down to the history store
        Optional<FileHistory> fetched = fileForTask(task);
        FileHistory file;
        if (fetched.isPresent()) {
            file = fetched.get();
            if (file.isEnqueuedForUpload() || file.isEnqueuedForRemoteCheck()) {
                return;
            }
        } else {
            file = new FileHistory(task.file());
        }

        uploadQueue.push(task);
        file.setEnqueuedForUpload(true);
        fileStore.store(file);
    }

    @Override
    public synchronized void recordUploadResult(UploadTask task, UploadAttempt result) throws InvalidStateException {
        Optional<FileHistory> fetched = fileForTask(task);
        if (!fetched.isPresent()) {
            throw new InvalidStateException("No file record found for task " +  task.toString());
        }
        FileHistory history = fetched.get();
        
        UploadAttempt withId = addUploadAttempt(history, result);
        // there's a double write here, but we need the upload attempt id
        switch (result.uploadResult()) {
        case SUCCESS:
            remoteCheckQueue.push(new RemoteCheckTask(task.file(), createParameterMap(withId)));
            uploadQueue.remove(task);
            fileStore.successfulUpload(history.file());
            break;
        case FAILURE:
        case UNKNOWN:
        default:
            uploadQueue.push(task);
            break;
        }
    }

    private UploadAttempt addUploadAttempt(FileHistory history, UploadAttempt result) {
        return fileStore.addUploadAttempt(history.file(), result);
    }

    private ImmutableMap<String, String> createParameterMap(UploadAttempt result) {
        return ImmutableMap.<String, String>builder()
                .putAll(result.uploadDetails())
                .put(UPLOAD_TIME_KEY, String.valueOf(result.uploadTime().getMillis()))
                .put(ATTEMPT_ID_KEY, String.valueOf(result.id()))
                .build();
    }

    @Override
    public synchronized void recordRemoteCheckResult(RemoteCheckTask task, RemoteCheckResult result) throws InvalidStateException {
        Optional<FileHistory> fetched = fileForTask(task);
        if (!fetched.isPresent()) {
            throw new InvalidStateException("No file record found for task " +  task.toString());
        }
        FileHistory file = fetched.get();
        
        boolean isFailure = false;
        switch (result.result()) {
        case SUCCESS:
            remoteCheckQueue.remove(task);
            file.setEnqueuedForRemoteCheck(false);
            break;
        case FAILURE:
            remoteCheckQueue.remove(task);
            file.setEnqueuedForRemoteCheck(false);
            isFailure = true;
            break;
        case UNKNOWN:
        default:
            remoteCheckQueue.push(task);
            break;
        }
        updateUploadAttempt(file, task, result);
        fileStore.store(file);
        if (isFailure) {
            enqueueUploadTask(new UploadTask(file.file()));
        }
    }

    private void updateUploadAttempt(FileHistory file, RemoteCheckTask task, RemoteCheckResult result) throws InvalidStateException {
        Long attemptId = Long.valueOf(task.uploadDetails().get(ATTEMPT_ID_KEY));
        Optional<UploadAttempt> attempt = file.getAttempt(attemptId);
        if (!attempt.isPresent()) {
            throw new InvalidStateException("attempted update of upload record without id " + String.valueOf(attemptId) + " " + file.toString());
        }
        UploadAttempt updatedAttempt = updateAttempt(attempt.get(), result);
        file.replaceAttempt(updatedAttempt);
    }
    

    private UploadAttempt updateAttempt(UploadAttempt upload, RemoteCheckResult result) {
        switch(result.result()) {
        case FAILURE:
            return UploadAttempt.failedRemoteCheck(upload, result.message());
        case SUCCESS:
            return UploadAttempt.successfulRemoteCheck(upload);
        case UNKNOWN:
            return UploadAttempt.unknownRemoteCheck(upload, result.message());
        default:
            return new UploadAttempt(upload.id(), upload.uploadTime(), upload.uploadResult(), upload.uploadDetails(), 
                    result.result(), result.message());
        }
    }

    private Optional<FileHistory> fileForTask(QueueTask task) {
        return fileStore.fetch(task.file());
    }
}
