package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.Clock;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;


public class TaskUpdatingResultHandler implements ResultHandler {

    private final Clock clock;
    private final TaskStore taskStore;
    private final JAXBContext context;
    
    public TaskUpdatingResultHandler(Clock clock, TaskStore taskStore) throws JAXBException {
        this.clock = checkNotNull(clock);
        this.taskStore = checkNotNull(taskStore);
        this.context = JAXBContext.newInstance("com.youview.refdata.schemas.youviewstatusreport._2010_12_07");
    }

    @Override
    public void handleTransactionResult(Task task, YouViewResult result) {
        if (result.isSuccess()) {
            taskStore.updateWithRemoteId(task.id(), Status.ACCEPTED, result.result(), clock.now());
        } else {
            Response response = new Response(Status.REJECTED, result.result(), clock.now());
            taskStore.updateWithResponse(task.id(), response);
        }
    }
    
    @Override
    public void handleRemoteCheckResult(Task task, YouViewResult result) {
        Status status;
        if (result.isSuccess()) {
            status = parseStatusFromResult(result.result());
        } else {
            status = Status.REJECTED;
        }
        Response response = new Response(status, result.result(), clock.now());
        taskStore.updateWithResponse(task.id(), response);
    }
    
    private Status parseStatusFromResult(String result) {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StatusReport report = (StatusReport) unmarshaller.unmarshal(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
            TransactionReportType txnReport = Iterables.getOnlyElement(report.getTransactionReport());

            switch (txnReport.getState()) {
            case ACCEPTED:
                return Status.ACCEPTED;
            case COMMITTED:
                return Status.COMMITTED;
            case COMMITTING:
                return Status.COMMITTING;
            case FAILED:
                return Status.FAILED;
            case PUBLISHED:
                return Status.PUBLISHED;
            case PUBLISHING:
                return Status.PUBLISHING;
            case QUARANTINED:
                return Status.QUARANTINED;
            case VALIDATING:
                return Status.VALIDATING;
            default:
                return Status.UNKNOWN;
            }
        } catch (JAXBException e) {
            throw Throwables.propagate(e);
        }
    }
}
