package org.atlasapi.feeds.youview.upload;

import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.junit.Test;
import org.mockito.Mockito;

import tva.mpeg7._2008.TextualType;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class TaskUpdatingResultHandlerTest {

    private static final String CONTENT_URI = "contentUri";
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final String ELEMENT_ID = "element_id";
    
    private Clock clock = new TimeMachine();
    private TaskStore taskStore = Mockito.mock(TaskStore.class);
    private JAXBContext context = JAXBContext.newInstance("com.youview.refdata.schemas.youviewstatusreport._2010_12_07");
    
    private final ResultHandler handler;
    
    public TaskUpdatingResultHandlerTest() throws JAXBException {
        this.handler = new TaskUpdatingResultHandler(clock, taskStore);
    }
    
    @Test
    public void testSuccessfulUploadUpdatesTaskAsAccepted() {
        long taskId = 1234l;
        Task task = createTaskWithId(taskId);
        String remoteId = "remoteId";
        YouViewResult result = YouViewResult.success(remoteId);
        
        handler.handleTransactionResult(task, result);
        
        verify(taskStore).updateWithRemoteId(taskId, Status.ACCEPTED, remoteId, clock.now());
    }
    
    @Test
    public void testFailedUploadUpdatesTaskWithRejectedResponse() {
        long taskId = 1234l;
        Task task = createTaskWithId(taskId);
        String failureMsg = "Something went wrong";
        YouViewResult result = YouViewResult.failure(failureMsg);
        
        handler.handleTransactionResult(task, result);
        
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
        YouViewResult result = YouViewResult.success(statusReportStr);
        
        handler.handleRemoteCheckResult(task, result);
        
        Response response = new Response(Status.PUBLISHED, statusReportStr, clock.now());
        verify(taskStore).updateWithResponse(taskId, response);
    }

    @Test
    public void testFailedRemoteCheckUpdatesTaskWithRejectedResponse() {
        long taskId = 1234l;
        Task task = createTaskWithId(taskId);
        String failureMsg = "failure";
        YouViewResult result = YouViewResult.failure(failureMsg);
        
        handler.handleRemoteCheckResult(task, result);
        
        Response response = new Response(Status.REJECTED, failureMsg, clock.now());
        verify(taskStore).updateWithResponse(taskId, response);
    }

    private Task createTaskWithId(long id) {
        return Task.builder()
                .withId(id)
                .withAction(Action.UPDATE)
                .withStatus(Status.NEW)
                .withContent(CONTENT_URI)
                .withPublisher(PUBLISHER)
                .withElementType(TVAElementType.VERSION)
                .withElementId(ELEMENT_ID)
                .build();
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
