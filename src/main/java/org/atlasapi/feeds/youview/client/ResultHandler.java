package org.atlasapi.feeds.youview.client;

import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.reporting.telescope.AtlasFeedsTelescopeProxy;

public interface ResultHandler {

    void handleTransactionResult(Task task, YouViewResult result, AtlasFeedsTelescopeProxy telescope);
    void handleRemoteCheckResult(Task task, YouViewResult result);
    void registerReportHandler(YouViewReportHandler reportHandler);
}
