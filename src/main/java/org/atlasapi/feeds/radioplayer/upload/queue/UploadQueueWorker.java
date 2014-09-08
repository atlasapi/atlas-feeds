package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.atlasapi.feeds.upload.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.time.Clock;


public class UploadQueueWorker extends QueueWorker<UploadTask> {
    
    public static final String ERROR_KEY = "error";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final UploadServicesSupplier uploaderSupplier;
    private final Clock clock;
    private final UploadManager stateUpdater;
    private final UploadCreator uploadCreator;
    
    public UploadQueueWorker(TaskQueue<UploadTask> uploadQueue, UploadServicesSupplier uploaderSupplier, 
            Clock clock, UploadCreator uploadCreator, UploadManager stateUpdater) {
        super(uploadQueue);
        this.uploaderSupplier = checkNotNull(uploaderSupplier);
        this.clock = checkNotNull(clock);
        this.uploadCreator = checkNotNull(uploadCreator);
        this.stateUpdater = checkNotNull(stateUpdater);
    }

    @Override
    public void processTask(UploadTask task) {
        UploadAttempt result = upload(task);
        recordResult(task, result);
    }

    // N.B. this code assumes that any upload-specific timestamps are related to UPLOAD time, rather than
    // the time at which the task was enqueued.
    private UploadAttempt upload(UploadTask task) {
        try {
            final FileUpload upload = uploadCreator.createUpload(task.service(), task.type(), task.date());
            Optional<FileUploader> uploader = uploaderSupplier.get(task.uploadService(), clock.now(), task.type());
            if (!uploader.isPresent()) {
                return logAndReturnFailure(String.format("No uploader found for remote service %s", task.uploadService()), Optional.<Exception>absent());
            }
            return performUpload(task, upload, uploader.get());
        } catch (IOException e) {
            return logAndReturnFailure("Error on file creation: {}", Optional.of(e));
        }
    }

    private UploadAttempt performUpload(UploadTask task, final FileUpload upload,
            FileUploader uploader) {
        try {
            return uploader.upload(upload);
        } catch (Exception e) {
            return logAndReturnFailure(String.format("Error on upload for remote service %s, task %s", task.uploadService(), task), Optional.of(e));
        }
    }

    private void recordResult(UploadTask task, UploadAttempt result) {
        try {
            stateUpdater.recordUploadResult(task, result);
        } catch (InvalidStateException e) {
            log.error("unable to update state for task {}", task, e);
        }
    }
    
    private UploadAttempt logAndReturnFailure(String message, Optional<? extends Exception> ex) {
        if (ex.isPresent()) {
            log.error(message, ex.get());
            return UploadAttempt.failedUpload(clock.now(), ImmutableMap.of(ERROR_KEY, message + ": " + ex.get().toString()));
        } else {
            log.error(message);
            return UploadAttempt.failedUpload(clock.now(), ImmutableMap.of(ERROR_KEY, message));
        }
    }
}
