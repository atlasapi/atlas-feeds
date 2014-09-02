package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.radioplayer.upload.queue.QueueBasedUploadManager.UPLOAD_TIME_KEY;

import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;


public class RemoteCheckQueueWorker extends QueueWorker<RemoteCheckTask> {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final RemoteCheckerSupplier remoteCheckers;
    private final UploadManager stateUpdater;
    
    public RemoteCheckQueueWorker(TaskQueue<RemoteCheckTask> remoteCheckQueue, 
            RemoteCheckerSupplier remoteCheckers, UploadManager stateUpdater) {
        super(remoteCheckQueue);
        this.remoteCheckers = checkNotNull(remoteCheckers);
        this.stateUpdater = checkNotNull(stateUpdater);
    }

    @Override
    public void processTask(RemoteCheckTask task) {
        try {
            RemoteCheckResult result = checkRemote(task);
            recordResult(task, result);
        } catch (InvalidStateException e) {
            log.error("error recording remote check result for task: {}", task);
        }
    }

    private RemoteCheckResult checkRemote(final RemoteCheckTask task) {
        DateTime uploadTime = parseUploadTime(task.getParameter(UPLOAD_TIME_KEY).get());
        Optional<RemoteCheckService> checker = remoteCheckers.get(task.uploadService(), uploadTime, task.type());
        if (checker.isPresent()) {
            try {
                return checker.get().check(task);
            } catch (RemoteCheckException e) {
                log.error(String.format("Error on remote check for service %s, task %s: {}", task.uploadService(), task), e);
                return RemoteCheckResult.failure(String.valueOf(e));
            }
        } else {
            log.error("No checker found for remote service {}", task.uploadService());
            return RemoteCheckResult.failure(String.format("No uploader found for remote service %s", task.uploadService()));
        }
    }

    private DateTime parseUploadTime(final String uploadTime) {
        return new DateTime(Long.valueOf(uploadTime));
    }

    private void recordResult(RemoteCheckTask task, RemoteCheckResult result) throws InvalidStateException {
        stateUpdater.recordRemoteCheckResult(task, result);
    }
}
