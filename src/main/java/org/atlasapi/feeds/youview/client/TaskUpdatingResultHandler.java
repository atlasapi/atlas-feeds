package org.atlasapi.feeds.youview.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class TaskUpdatingResultHandler implements ResultHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskUpdatingResultHandler.class);

    private final TaskStore taskStore;
    private final JAXBContext context;

    private YouViewReportHandler reportHandler;
    private TimeCounter successfullCounter;
    private TimeCounter unsuccessfulCounter;

    public TaskUpdatingResultHandler(TaskStore taskStore, MetricRegistry metricRegistry)
            throws JAXBException {
        this.taskStore = checkNotNull(taskStore);
        this.context = JAXBContext.newInstance("com.youview.refdata.schemas.youviewstatusreport._2010_12_07");
        this.successfullCounter = metricRegistry.register(
                "YouviewSuccessfullTasks",
                new TimeCounter(4, TimeUnit.HOURS)
        );
        log.info("Created successfull counter");
        this.unsuccessfulCounter = metricRegistry.register(
                "YouviewUnsuccessfullTasks",
                new TimeCounter(4, TimeUnit.HOURS)
        );
        log.info("Created unsuccessfull counter");
        log.info("Metric registry contains {} metrics: {}",
                metricRegistry.getMetrics().size(),
                metricRegistry.getMetrics().keySet()
        );
    }

    /**
     * This handles a couple of different cases. The simplest is success, where the task is moved
     * forwards and a remote ID and upload time are written.
     * <p>
     * Next simplest is 400, which is YouView parlance for an error in the uploaded payload. This
     * results in the Task being failed.
     * <p>
     * Any other response is treated as an erroneous upload error, and the response is written to
     * the task with a status of PENDING, so it will be reuploaded. The exception to this is if the
     * retry count has been exceeded, in which case the Task will be failed.
     */
    @Override
    public void handleTransactionResult(
            Task task,
            YouViewResult result,
            FeedsTelescopeReporter telescope
    ) {
        //get the payload so we can report it to telescope
        String payload = task.payload().isPresent()
                         ? task.payload().get().payload()
                         : "No Payload";
        log.info("handling transaction result for {}" + task.atlasDbId());

        if (result.isSuccess()) {
            telescope.reportSuccessfulEvent(task.atlasDbId(), payload);
            taskStore.updateWithRemoteId(
                    task.id(),
                    Status.ACCEPTED,
                    result.result(),
                    result.uploadTime()
            );
            successfullCounter.inc();
        } else {
            Response response = new Response(Status.REJECTED, result.result(), result.uploadTime());
            telescope.reportFailedEventWithError(
                    String.format("Content was rejected. (%s)", result.result()),
                    payload
            );
            taskStore.updateWithResponse(task.id(), response);
            unsuccessfulCounter.inc();
        }
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

    @Override
    public void registerReportHandler(YouViewReportHandler reportHandler) {
        this.reportHandler = checkNotNull(reportHandler);
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
        StatusReport report = (StatusReport) unmarshaller.unmarshal(
                new ByteArrayInputStream(
                        result.getBytes(StandardCharsets.UTF_8)
                )
        );
        return Iterables.getOnlyElement(report.getTransactionReport());
    }

    public static class TimeCounter implements Metric, Counting {

        private final Histogram histogram;

        public TimeCounter(long time, TimeUnit unit) {
            histogram = new Histogram(new SlidingTimeWindowReservoir(time, unit));
        }

        public void inc() {
            histogram.update(1L);
        }

        @Override
        public long getCount() {
            return histogram.getSnapshot().size();
        }
    }
}
