package org.atlasapi.feeds.youview.client;

import org.atlasapi.feeds.tasks.Task;


public interface ResultHandler {

    void handleTransactionResult(Task task, YouViewResult result);
    void handleRemoteCheckResult(Task task, YouViewResult result);
    void registerReportHandler(YouViewReportHandler reportHandler);
}
