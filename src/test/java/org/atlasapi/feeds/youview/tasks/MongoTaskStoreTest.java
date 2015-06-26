package org.atlasapi.feeds.youview.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;

import org.atlasapi.feeds.youview.tasks.persistence.MongoTaskStore;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.query.Selection;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.TimeMachine;


public class MongoTaskStoreTest {
    
    private static final Publisher publisher = Publisher.METABROADCAST;

    private DateTime time = new DateTime(2012, 1, 1, 0, 0, 0).withZone(DateTimeZones.UTC);
    private Clock clock = new TimeMachine(time);
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    
    private final TaskStore store = new MongoTaskStore(mongo);

    // TODO separate test class for id-setting store
    
    @Test
    public void testSavingAndRetrievingTaskReturnsSameTask() {
        long taskId = 1234l;
        Task task = createAndStoreTask(taskId, "filmUri", Status.ACCEPTED);
        
        Task fetched = store.taskFor(taskId).get();
        
        assertEquals(task, fetched);
        
        assertEquals(task.content(), fetched.content());
        assertEquals(task.uploadTime(), fetched.uploadTime());
        assertEquals(task.status(), fetched.status());
    }
    
    @Test
    public void testUpdatingTaskWithStatusUpdatesStatusCorrectly() {
        long taskId = 1234l;
        Status initialStatus = Status.ACCEPTED;
        Task task = createAndStoreTask(taskId, "filmUri", initialStatus);
        
        store.save(task);
        
        Task fetched = store.taskFor(taskId).get();
        
        assertEquals(initialStatus, fetched.status());
        
        Status updatedStatus = Status.PUBLISHED;
        store.updateWithStatus(taskId, updatedStatus);
        
        fetched = store.taskFor(taskId).get();
        
        assertEquals(updatedStatus, fetched.status());
    }
    
    @Test
    public void testUpdatingTaskWithRemoteIdUpdatesStatusAndUploadTimeCorrectly() {
        long taskId = 1234l;
        
        Task task = createAndStoreTask(taskId, "filmUri", Status.ACCEPTED);
        store.save(task);
        
        Task fetched = store.taskFor(taskId).get();
        
        assertFalse("there should not be a remote ID set initially", fetched.remoteId().isPresent());
        
        String remoteId = "txn_id";
        DateTime uploadTime = clock.now();
        Status newStatus = Status.PUBLISHED;
        store.updateWithRemoteId(taskId, newStatus, remoteId, uploadTime);
        
        fetched = store.taskFor(taskId).get();
        
        assertEquals(remoteId, fetched.remoteId().get());
        assertEquals(uploadTime, fetched.uploadTime().get());
        assertEquals(newStatus, fetched.status());
    }
    
    @Test
    public void testUpdatingTaskWithResponseUpdatesResponsesAndStatusCorrectly() {
        long taskId = 1234l;
        Task task = createAndStoreTask(taskId, "filmUri", Status.ACCEPTED);
        
        store.save(task);
        
        Task fetched = store.taskFor(taskId).get();
        
        assertTrue("Remote Responses should be empty if no responses have been written", fetched.remoteResponses().isEmpty());
        
        Response response = new Response(Status.COMMITTED, "payload", clock.now());
        store.updateWithResponse(taskId, response);
        
        fetched = store.taskFor(taskId).get();
        
        assertEquals(response.status(), fetched.status());
        assertEquals(response, Iterables.getOnlyElement(fetched.remoteResponses()));
    }
    
    @Test
    public void testUpdatingTaskWithMultipleResponsesUpdatesResponsesAndStatusCorrectly() {
        long taskId = 1234l;
        Task task = createAndStoreTask(taskId, "filmUri", Status.ACCEPTED);
        
        store.save(task);
        
        Task fetched = store.taskFor(taskId).get();
        
        assertTrue("Remote Responses should be empty if no responses have been written", fetched.remoteResponses().isEmpty());
        
        Response firstResponse = new Response(Status.COMMITTED, "payload", clock.now());
        store.updateWithResponse(taskId, firstResponse);
        
        Response finalResponse = new Response(Status.PUBLISHED, "payload", clock.now().plusMinutes(1));
        store.updateWithResponse(taskId, finalResponse);
        
        fetched = store.taskFor(taskId).get();
        
        assertEquals(finalResponse.status(), fetched.status());
        assertEquals(ImmutableSet.of(firstResponse, finalResponse), fetched.remoteResponses());
    }
    
    @Test
    public void testLimitAndOffsetQueryCorrectlyTrimsReturnedSetOfTransactions() {
        Task t1 = createAndStoreTask(1234l, "", Status.ACCEPTED);
        Task t2 = createAndStoreTask(2345l, "", Status.ACCEPTED);
        Task t3 = createAndStoreTask(3456l, "", Status.ACCEPTED);
        
        Selection selection = selectionWithParams(100, 0);
        TaskQuery query = TaskQuery.builder(selection, publisher).build();
        Iterable<Task> allTransactions = store.allTasks(query);
        
        assertEquals(ImmutableList.of(t1, t2, t3), ImmutableList.copyOf(allTransactions));
        
        selection = selectionWithParams(1, 1);
        query = TaskQuery.builder(selection, publisher).build();
        allTransactions = store.allTasks(query);
        
        assertEquals(Iterables.getOnlyElement(allTransactions), t2);
    }
    
    @Test
    public void testRetrievingTasksByStatusFiltersReturnedTasksCorrectly() {
        Task desired = createAndStoreTask(1234l, "", Status.NEW);
        createAndStoreTask(2345l, "", Status.FAILED);
        createAndStoreTask(3456l, "", Status.ACCEPTED);
        
        Iterable<Task> fetched = store.allTasks(Status.NEW);
        
        assertEquals(desired, Iterables.getOnlyElement(fetched));
    }
    
    @Test
    public void testContentUriQueryCorrectlyTrimsReturnedSetOfTransactions() {
        
        String desiredUri = "filterByMe";
        String contentUri = "not part of filter";
        String anotherContentUri = "also not part of filter";
        
        Task desiredTask = createAndStoreTask(1234l, desiredUri, Status.ACCEPTED);
        createAndStoreTask(2345l, contentUri, Status.ACCEPTED);
        createAndStoreTask(3456l, anotherContentUri, Status.ACCEPTED);
        
        Selection selection = selectionWithParams(100, 0);
        TaskQuery query = TaskQuery.builder(selection, publisher)
                .withContentUri(desiredUri)
                .build();
        Iterable<Task> allTransactions = store.allTasks(query);
        
        assertEquals(desiredTask, Iterables.getOnlyElement(allTransactions));
    }
    
    @Test
    public void testTransactionIdQueryCorrectlyTrimsReturnedSetOfTransactions() {
        
        Task desiredTask = createAndStoreTask(1234l, "content", Status.ACCEPTED);
        Task anotherTask = createAndStoreTask(2345l, "content", Status.ACCEPTED);
        Task thirdTask = createAndStoreTask(3456l, "content", Status.ACCEPTED);
        
        String transactionId = "desiredTxn";
        
        store.updateWithRemoteId(desiredTask.id(), Status.COMMITTED, transactionId, clock.now());
        store.updateWithRemoteId(anotherTask.id(), Status.COMMITTED, "txn2", clock.now());
        store.updateWithRemoteId(thirdTask.id(), Status.COMMITTED, "txn3", clock.now());
        
        Selection selection = selectionWithParams(100, 0);
        TaskQuery query = TaskQuery.builder(selection, publisher)
                .withRemoteId(transactionId)
                .build();
        Iterable<Task> allTransactions = store.allTasks(query);
        
        assertEquals(desiredTask, Iterables.getOnlyElement(allTransactions));
    }
    
    @Test
    public void testTransactionStatusQueryCorrectlyTrimsReturnedSetOfTransactions() {

        Status status = Status.ACCEPTED;
        
        Task desiredTask = createAndStoreTask(1234l, "content", status);
        createAndStoreTask(2345l, "content", Status.COMMITTED);
        
        Selection selection = selectionWithParams(100, 0);
        TaskQuery query = TaskQuery.builder(selection, publisher)
                .withTaskStatus(status)
                .build();
        Iterable<Task> allTransactions = store.allTasks(query);
        
        assertEquals(desiredTask, Iterables.getOnlyElement(allTransactions));
    }
    
    @Test
    public void testRegexMatchOnContentUriQueryCorrectlyTrimsReturnedSetOfTransactions() {
        
        String desiredUri = "filterByMe";
        String contentUri = "not part of filter";
        String anotherContentUri = "also not part of filter";
        
        Task desiredTask = createAndStoreTask(1234l, desiredUri, Status.ACCEPTED);
        createAndStoreTask(2345l, contentUri, Status.ACCEPTED);
        createAndStoreTask(3456l, anotherContentUri, Status.ACCEPTED);
        
        Selection selection = selectionWithParams(100, 0);
        TaskQuery query = TaskQuery.builder(selection, publisher)
                .withContentUri("filterBy")
                .build();
        Iterable<Task> allTransactions = store.allTasks(query);
        
        assertEquals(desiredTask, Iterables.getOnlyElement(allTransactions));
    }
    
    @Test
    public void testRegexMatchOnTransactionIdQueryCorrectlyTrimsReturnedSetOfTransactions() {
        
        Task desiredTask = createAndStoreTask(1234l, "content", Status.ACCEPTED);
        Task anotherTask = createAndStoreTask(2345l, "notdesiredcontent", Status.ACCEPTED);
        Task thirdTask = createAndStoreTask(3456l, "content", Status.ACCEPTED);
        
        String transactionId = "desiredTxn";
        
        store.updateWithRemoteId(desiredTask.id(), Status.COMMITTED, transactionId, clock.now());
        store.updateWithRemoteId(anotherTask.id(), Status.COMMITTED, "txn2", clock.now());
        store.updateWithRemoteId(thirdTask.id(), Status.COMMITTED, "txn3", clock.now());
        
        Selection selection = selectionWithParams(100, 0);
        TaskQuery query = TaskQuery.builder(selection, publisher)
                .withRemoteId("desired")
                .build();
        Iterable<Task> allTransactions = store.allTasks(query);
        
        assertEquals(desiredTask, Iterables.getOnlyElement(allTransactions));
    }
    
    @Test
    public void testRegexMatchOnContentUriIsPrefixQuery() {
        
        Task desiredTask = createAndStoreTask(1234l, "desireduri", Status.ACCEPTED);
        Task notDesiredTask = createAndStoreTask(1235l, "notdesireduri", Status.ACCEPTED);
        
        Selection selection = selectionWithParams(100, 0);
        TaskQuery query = TaskQuery
                            .builder(selection, publisher)
                            .withContentUri("uri")
                            .build();
        
        assertFalse(store.allTasks(query).iterator().hasNext());
        
        TaskQuery successfulQuery = TaskQuery
                                        .builder(selection, publisher)
                                        .withContentUri("desireduri")
                                        .build();
        
        assertEquals(desiredTask, Iterables.getOnlyElement(store.allTasks(successfulQuery)));
        
    }
    
    private Task createAndStoreTask(long taskId, String content, Status status) {
        Task txn = createTransaction(
                taskId, 
                content,
                status
        );
        store.save(txn);
        return txn;
    }

    private Selection selectionWithParams(int limit, int offset) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        Mockito.when(request.getParameter("limit")).thenReturn(String.valueOf(limit));
        Mockito.when(request.getParameter("offset")).thenReturn(String.valueOf(offset));
        
        return Selection.builder().build(request);
    }

    private Task createTransaction(long taskId, String content,
            Status status) {
        return Task.builder()
                .withId(taskId)
                .withPublisher(publisher)
                .withContent(content)
                .withElementType(TVAElementType.ITEM)
                .withElementId("elementId")
                .withAction(Action.UPDATE)
                .withStatus(status)
                .build();
    }
}
