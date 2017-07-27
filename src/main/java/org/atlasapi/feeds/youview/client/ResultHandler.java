package org.atlasapi.feeds.youview.client;

import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.telescope.TelescopeProxy;

public interface ResultHandler {

    void handleTransactionResult(Task task, YouViewResult result, TelescopeProxy telescope);
    void handleRemoteCheckResult(Task task, YouViewResult result);
    void registerReportHandler(YouViewReportHandler reportHandler);
}
