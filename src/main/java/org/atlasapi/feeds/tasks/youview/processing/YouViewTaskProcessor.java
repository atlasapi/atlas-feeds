package org.atlasapi.feeds.tasks.youview.processing;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.atlasapi.feeds.RateLimiter;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.youview.client.ResultHandler;
import org.atlasapi.feeds.youview.client.YouViewClient;
import org.atlasapi.feeds.youview.client.YouViewClientException;
import org.atlasapi.feeds.youview.client.YouViewResult;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;

public class YouViewTaskProcessor implements TaskProcessor {

    private final Logger log = LoggerFactory.getLogger(YouViewTaskProcessor.class);
    private final YouViewClient client;
    private final RevokedContentStore revocationStore;
    private final ResultHandler resultHandler;
    private final TaskStore taskStore;
    private final RateLimiter rateLimiter = RateLimiter.create(35); //limit the requests per second

    public YouViewTaskProcessor(
            YouViewClient client,
            ResultHandler resultHandler,
            RevokedContentStore revocationStore,
            TaskStore taskStore
    ) {
        this.client = checkNotNull(client);
        this.resultHandler = checkNotNull(resultHandler);
        this.revocationStore = checkNotNull(revocationStore);
        this.taskStore = checkNotNull(taskStore);
    }

    @Override
    public void process(Task task, FeedsTelescopeReporter telescope) {
        checkArgument(
                YOUVIEW.equals(task.destination().type()),
                "task type " + task.destination().type() + " invalid, expected " + YOUVIEW.name()
        );

        try {
            switch (task.action()) {
            case UPDATE:
                rateLimiter.acquire();
                processUpdate(task, telescope);
                break;
            case DELETE:
                rateLimiter.acquire();
                processDelete(task, telescope);
                break;
            default:
                throw new IllegalStateException("action " + task.action().name()
                                                + " not recognised for task " + task.id());
            }
        } catch (Exception e) {
            log.error("Error processing Task {}", task.id(), e);
            telescope.reportFailedEventFromTask(
                    task,
                    "Failed to process taskId=" + task.id()
                    + ". destination " + task.destination()
                    + ". atlasId=" + task.atlasDbId()
                    + ". payload present=" + task.payload().isPresent()
                    + " (" + e.getMessage() + ") "
            );
            setFailed(task, e);
        }
    }

    private boolean isRevoked(String contentUri) {
        return revocationStore.isRevoked(contentUri);
    }

    private void processUpdate(Task task, FeedsTelescopeReporter telescope) {
        if (!task.payload().isPresent()) { //If you want remove this, check for any .get() down the line.
            telescope.reportFailedEventFromTask(
                    task,
                    "Failed to " + task.action().name() + " taskId=" + task.id()
                    + ". destination " + task.destination()
                    + ". atlasId=" + task.atlasDbId()
                    + ". There was no payload."
            );
            setFailed(task, new Exception("Lack of payload."));
            return;
        }

        YouViewDestination destination = (YouViewDestination) task.destination();
        if (isRevoked(destination.contentUri())) {
            if (task.isManuallyCreated()) {
                revocationStore.unrevoke(destination.contentUri());
            } else {
                log.info(
                        "content {} is revoked, not {}ing",
                        destination.contentUri(),
                        task.action().name()
                );
                telescope.reportSuccessfulEventWithWarning(
                        task,
                        "Content " + destination.contentUri() + " is revoked. Cannot " + task.action().name()
                );
                setFailed(task, new Exception("Content is revoked."));
                return;
            }
        }

        YouViewResult uploadResult = client.upload(task.payload().get());
        resultHandler.handleTransactionResult(task, uploadResult, telescope);
    }

    private void setFailed(Task task) {
        setFailed(task, null);
    }

    private void setFailed(Task task, Exception e) {
        taskStore.updateWithStatus(task.id(), Status.FAILED);
        if (e != null) {
            taskStore.updateWithLastError(task.id(), exceptionToString(e));
        }
    }

    private String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return e.getMessage() + " " + sw.toString();
    }

    // No need to check for revocation for deletes, as deleting revoked content doesn't really matter
    // It also allows the deletes resulting from a revoke to go through unhindered.
    private void processDelete(Task task, FeedsTelescopeReporter telescope) {
        YouViewDestination destination = (YouViewDestination) task.destination();
        YouViewResult deleteResult = client.delete(destination.elementId());
        resultHandler.handleTransactionResult(task, deleteResult, telescope);
    }

    @Override
    public void checkRemoteStatusOf(Task task) {
        checkArgument(
                task.remoteId().isPresent(),
                "no transaction id present for task " + task.id() + ", cannot check status"
        );
        YouViewResult result;
        try {
            rateLimiter.acquire();
            result = client.checkRemoteStatus(task.remoteId().get());
        } catch (YouViewClientException e){
            log.error("", e);
            return;
        }
        resultHandler.handleRemoteCheckResult(task, result);
    }
}
