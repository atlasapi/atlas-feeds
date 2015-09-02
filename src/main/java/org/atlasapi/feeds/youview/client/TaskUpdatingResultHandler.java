package org.atlasapi.feeds.youview.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;


public class TaskUpdatingResultHandler implements ResultHandler {

    private static final Predicate<Response> IS_PENDING = new Predicate<Response>() {
        @Override
        public boolean apply(Response input) {
            return Status.PENDING.equals(input.status());
        }
    };
    private final TaskStore taskStore;
    private final int maxRetries;
    private final JAXBContext context;
    
    private YouViewReportHandler reportHandler;
    
    public TaskUpdatingResultHandler(TaskStore taskStore, Integer maxRetries) throws JAXBException {
        this.taskStore = checkNotNull(taskStore);
        this.maxRetries = checkNotNull(maxRetries);
        this.context = JAXBContext.newInstance("com.youview.refdata.schemas.youviewstatusreport._2010_12_07");
    }
    
    @Override
    public void registerReportHandler(YouViewReportHandler reportHandler) {
        this.reportHandler = checkNotNull(reportHandler);
    }

    /**
     * This handles a couple of different cases. The simplest is success, where the task is moved forwards
     * and a remote ID and upload time are written.
     * <p> 
     * Next simplest is 400, which is YouView parlance for an error in the uploaded payload. This results in
     * the Task being failed.
     * <p>
     * Any other response is treated as an erroneous upload error, and the response is written to the task with
     * a status of PENDING, so it will be reuploaded. The exception to this is if the retry count has been 
     * exceeded, in which case the Task will be failed. 
     */
    @Override
    public void handleTransactionResult(Task task, YouViewResult result) {
        if (result.isSuccess()) {
            taskStore.updateWithRemoteId(task.id(), Status.ACCEPTED, result.result(), result.uploadTime());
        } else {
            if (result.responseCode() != 400) {
                if (pendingResponses(task) < maxRetries) {
                    Response response = new Response(Status.PENDING, result.result(), result.uploadTime());
                    taskStore.updateWithResponse(task.id(), response);
                    return;
                }
            }
            Response response = new Response(Status.REJECTED, result.result(), result.uploadTime());
            taskStore.updateWithResponse(task.id(), response);
        }
    }

    private int pendingResponses(Task task) {
        return Iterables.size(Iterables.filter(task.remoteResponses(), IS_PENDING));
    }
    
    @Override
    public void handleRemoteCheckResult(Task task, YouViewResult result) {
        Status status;
        if (result.isSuccess()) {
            status = parseAndHandleStatusReport(result.result(), task);
        } else {
            status = Status.REJECTED;
        }
        Response response = new Response(status, result.result(), result.uploadTime());
        taskStore.updateWithResponse(task.id(), response);
    }
    
    private Status parseAndHandleStatusReport(String result, Task task) {
        try {
            TransactionReportType txnReport = parseReportFrom(result);
            if (reportHandler != null) {
                reportHandler.handle(txnReport, task);
            }
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

    private TransactionReportType parseReportFrom(String result) throws JAXBException {
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StatusReport report = (StatusReport) unmarshaller.unmarshal(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
        TransactionReportType txnReport = Iterables.getOnlyElement(report.getTransactionReport());
        return txnReport;
    }
}
