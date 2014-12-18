package org.atlasapi.feeds.youview.upload;

import org.atlasapi.feeds.youview.tasks.Task;


public interface ResultHandler {

    void handleTransactionResult(Task task, YouViewResult result);
    void handleRemoteCheckResult(Task task, YouViewResult result);
}
