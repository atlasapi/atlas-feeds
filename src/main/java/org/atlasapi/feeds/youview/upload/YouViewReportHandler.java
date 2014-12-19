package org.atlasapi.feeds.youview.upload;

import org.atlasapi.feeds.youview.tasks.Task;

import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;


public interface YouViewReportHandler {
    
    void handle(TransactionReportType report, Task task);
}
