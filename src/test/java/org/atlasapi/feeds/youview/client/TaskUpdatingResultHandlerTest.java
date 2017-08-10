package org.atlasapi.feeds.youview.client;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination;
import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.reporting.telescope.FeedsTelescopeReporter;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;

import com.codahale.metrics.MetricRegistry;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import tva.mpeg7._2008.TextualType;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class TaskUpdatingResultHandlerTest {

    private static final String CONTENT_URI = "contentUri";
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final String ELEMENT_ID = "element_id";
    
    private Clock clock = new TimeMachine();
    private TaskStore taskStore = mock(TaskStore.class);
    private YouViewReportHandler reportHandler = mock(YouViewReportHandler.class);
    private JAXBContext context = JAXBContext.newInstance("com.youview.refdata.schemas.youviewstatusreport._2010_12_07");

    @Mock FeedsTelescopeReporter telescope;
    @Mock MetricRegistry metricRegistry;
    
    private final ResultHandler handler = new TaskUpdatingResultHandler(taskStore, metricRegistry);
    
    public TaskUpdatingResultHandlerTest() throws JAXBException {
        handler.registerReportHandler(reportHandler);
    }
    
    @Test
    public void testSuccessfulUploadUpdatesTaskAsAccepted() {
        long taskId = 1234l;
        Task task = createTaskWithId(taskId);
        String remoteId = "remoteId";
        YouViewResult result = YouViewResult.success(remoteId, clock.now(), SC_ACCEPTED);
        
        handler.handleTransactionResult(task, result, telescope);
        
        verify(taskStore).updateWithRemoteId(taskId, Status.ACCEPTED, remoteId, clock.now());
    }
    
    @Test
    public void testBadRequestFailedUploadUpdatesTaskWithRejectedResponse() {
        long taskId = 1234l;
        Task task = createTaskWithId(taskId);
        String failureMsg = "Something went wrong";
        YouViewResult result = YouViewResult.failure(failureMsg, clock.now(), SC_BAD_REQUEST);
        
        handler.handleTransactionResult(task, result, telescope);
        
        Response response = new Response(Status.REJECTED, failureMsg, clock.now());
        verify(taskStore).updateWithResponse(taskId, response);
    }

    @Test
    public void testSuccessfulRemoteCheckUpdatesTaskWithAppropriateResponse() throws JAXBException {
        long taskId = 1234l;
        Task task = createTaskWithId(taskId);
        
        JAXBElement<StatusReport> report = createStatusReport();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Marshaller marshaller = context.createMarshaller();
        marshaller.marshal(report, baos);
        
        String statusReportStr = baos.toString();
        YouViewResult result = YouViewResult.success(statusReportStr, clock.now(), SC_ACCEPTED);
        
        handler.handleRemoteCheckResult(task, result);
        
        Response response = new Response(Status.PUBLISHED, statusReportStr, clock.now());
        verify(taskStore).updateWithResponse(taskId, response);
    }

    @Test
    public void testFailedRemoteCheckUpdatesTaskWithRejectedResponse() {
        long taskId = 1234l;
        Task task = createTaskWithId(taskId);
        String failureMsg = "failure";
        YouViewResult result = YouViewResult.failure(failureMsg, clock.now(), SC_BAD_REQUEST);
        
        handler.handleRemoteCheckResult(task, result);
        
        Response response = new Response(Status.REJECTED, failureMsg, clock.now());
        verify(taskStore).updateWithResponse(taskId, response);
    }

    private Task createTaskWithId(long id) {
        return Task.builder()
                .withId(id)
                .withAction(Action.UPDATE)
                .withStatus(Status.NEW)
                .withCreated(clock.now())
                .withDestination(createDestination())
                .withPublisher(PUBLISHER)
                .build();
    }
    
    private Destination createDestination() {
        return new YouViewDestination(CONTENT_URI, TVAElementType.VERSION, ELEMENT_ID);
    }

    private JAXBElement<StatusReport> createStatusReport() {
        StatusReport report = new StatusReport();
        
        TransactionReportType txnReport = new TransactionReportType();
        
        FragmentReportType fragmentReport = new FragmentReportType();
        
        ControlledMessageType message = new ControlledMessageType();
        
        TextualType comment = new TextualType();
        
        comment.setValue("this fragment was AOK");
        
        message.setComment(comment);
        
        fragmentReport.getMessage().add(message);
        fragmentReport.setFragmentId("fragmentId");
        
        txnReport.getFragmentUpdateReport().add(fragmentReport);
        txnReport.setState(TransactionStateType.PUBLISHED);
        
        report.getTransactionReport().add(txnReport);
        
        return new JAXBElement<StatusReport>(
                new QName("http://refdata.youview.com/schemas/YouViewStatusReport/2010-12-07", "StatusReport"), 
                StatusReport.class, 
                null, 
                report
        );
    }
}
