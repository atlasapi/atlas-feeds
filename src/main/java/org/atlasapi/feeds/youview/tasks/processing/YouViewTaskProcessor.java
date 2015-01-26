package org.atlasapi.feeds.youview.tasks.processing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.tasks.Destination.DestinationType.YOUVIEW;

import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.YouViewDestination;
import org.atlasapi.feeds.youview.upload.ResultHandler;
import org.atlasapi.feeds.youview.upload.YouViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// analogous to the youview service
public class YouViewTaskProcessor implements TaskProcessor {
    
    private final Logger log = LoggerFactory.getLogger(YouViewTaskProcessor.class);
    private final YouViewClient client;
    private final RevokedContentStore revocationStore;
    private final ResultHandler resultHandler;
    
    public YouViewTaskProcessor(YouViewClient client, ResultHandler resultHandler, RevokedContentStore revocationStore) {
        this.client = checkNotNull(client);
        this.resultHandler = checkNotNull(resultHandler);
        this.revocationStore = checkNotNull(revocationStore);
    }

    @Override
    public void process(Task task) {
        checkArgument(
                YOUVIEW.equals(task.destination().type()), 
                "task type " + task.destination().type() + " invalid, expected " + YOUVIEW.name()
        );
        YouViewDestination destination = (YouViewDestination) task.destination();
        if (isRevoked(destination.contentUri())) {
            log.info("content {} is revoked, not {}ing", destination.contentUri(), task.action().name());
            return;
        }
        switch(task.action()) {
        case UPDATE:
            processUpdate(task);
            break;
        case DELETE:
            processDelete(task);
            break;
        default:
            throw new RuntimeException("action " + task.action().name() + " not recognised for task " + task.id());
        }
    }
    
    private boolean isRevoked(String contentUri) {
        return revocationStore.isRevoked(contentUri);
    }

    private void processUpdate(Task task) {
        checkArgument(task.payload().isPresent(), "no payload present for task " + task.id() + ", cannot upload");
        YouViewResult uploadResult = client.upload(task.payload().get());
        resultHandler.handleTransactionResult(task, uploadResult);
    }

    private void processDelete(Task task) {
        YouViewDestination destination = (YouViewDestination) task.destination();
        YouViewResult deleteResult = client.delete(destination.elementId());
        resultHandler.handleTransactionResult(task, deleteResult);
    }

    @Override
    public void checkRemoteStatusOf(Task task) {
        checkArgument(task.remoteId().isPresent(), "no transaction id present for task " + task.id() + ", cannot check status");
        YouViewResult result = client.checkRemoteStatus(task.remoteId().get());
        resultHandler.handleRemoteCheckResult(task, result);
    }
}
