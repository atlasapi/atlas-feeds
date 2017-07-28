package org.atlasapi.feeds.youview.client;

import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.telescope.TelescopeProxy1;

public interface ResultHandler {

    void handleTransactionResult(Task task, YouViewResult result, TelescopeProxy1 telescope);
    void handleRemoteCheckResult(Task task, YouViewResult result);
    void registerReportHandler(YouViewReportHandler reportHandler);
}
