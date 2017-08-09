package org.atlasapi.feeds.youview.client;

import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;

public interface ResultHandler {

    void handleTransactionResult(Task task, YouViewResult result, FeedsTelescopeReporter telescope);
    void handleRemoteCheckResult(Task task, YouViewResult result);
    void registerReportHandler(YouViewReportHandler reportHandler);
}
