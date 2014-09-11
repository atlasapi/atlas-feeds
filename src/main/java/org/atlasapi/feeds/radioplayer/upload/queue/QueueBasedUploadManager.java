package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.FileHistoryStore;
import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;


public class QueueBasedUploadManager implements UploadManager {
    
    static final String ATTEMPT_ID_KEY = "attemptId";
    static final String UPLOAD_TIME_KEY = "uploadTime";
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
        if (isEnqueued(task.file())) {
            return;
        }
        uploadQueue.push(task);
        createFileHistoryIfNotPresent(task.file());
    }

    private void createFileHistoryIfNotPresent(RadioPlayerFile file) {
        Optional<FileHistory> fetched = fileStore.fetch(file);
        if (!fetched.isPresent()) {
            fileStore.store(new FileHistory(file));
        }
    }


    private boolean isEnqueued(RadioPlayerFile file) {
        return uploadQueue.contains(file) || remoteCheckQueue.contains(file); 
    }

    @Override
    public synchronized void recordUploadResult(UploadTask task, UploadAttempt result) throws InvalidStateException {
        Optional<FileHistory> fetched = fileForTask(task);
        if (!fetched.isPresent()) {
            throw new InvalidStateException("No file record found for task " +  task.toString());
        }
        UploadAttempt withId = addUploadAttempt(fetched.get(), result);

        switch (result.uploadResult()) {
        case SUCCESS:
            remoteCheckQueue.push(new RemoteCheckTask(task.file(), createParamMapFromUpload(withId)));
            uploadQueue.remove(task);
            break;
        case FAILURE:
            uploadQueue.remove(task);
            break;
        case UNKNOWN:
        default:
            retryUpload(task);
            break;
        }
    }

    private void retryUpload(UploadTask task) {
        uploadQueue.push(task);
    }


    private UploadAttempt addUploadAttempt(FileHistory history, UploadAttempt result) {
        return fileStore.addUploadAttempt(history.file(), result);
    }

    private ImmutableMap<String, String> createParamMapFromUpload(UploadAttempt result) {
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
        updateUploadAttempt(file, task, result);
        fileStore.store(file);
        
        switch (result.result()) {
        case SUCCESS:
            remoteCheckQueue.remove(task);
            break;
        case FAILURE:
            retryUploadForRemoteCheckTask(task);
            break;
        case UNKNOWN:
        default:
            retryRemoteCheck(task);
            break;
        }
    }

    private void retryRemoteCheck(RemoteCheckTask task) {
        remoteCheckQueue.push(task);
    }

    private void retryUploadForRemoteCheckTask(RemoteCheckTask task) {
        remoteCheckQueue.remove(task);
        enqueueUploadTask(new UploadTask(task.file()));
    }


    private void updateUploadAttempt(FileHistory file, RemoteCheckTask task, RemoteCheckResult result) throws InvalidStateException {
        Long attemptId = Long.valueOf(task.getParameter(ATTEMPT_ID_KEY).get());
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
            return UploadAttempt.successfulRemoteCheck(upload, result.message());
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
