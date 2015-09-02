package org.atlasapi.feeds.youview.client;

import org.atlasapi.feeds.tasks.Task;

import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;


public interface YouViewReportHandler {
    
    void handle(TransactionReportType report, Task task);
}
