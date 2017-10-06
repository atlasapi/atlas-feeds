package org.atlasapi.feeds.tasks.persistence;

import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.TaskQuery;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.persistence.MongoTaskStore;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
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
    
    private static final Publisher PUBLISHER = Publisher.METABROADCAST;
    private static final DestinationType TYPE = DestinationType.YOUVIEW;

    private DateTime time = new DateTime(2012, 1, 1, 0, 0, 0).withZone(DateTimeZones.UTC);
    private Clock clock = new TimeMachine(time);
    private Payload DEFAULT_PAYLOAD = new Payload("payload", clock.now());
    
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    
    private final TaskStore store = new MongoTaskStore(mongo);

    @Test
    public void testSavingAndRetrievingTaskReturnsSameTask() {
        long taskId = 1234l;
        Task task = createAndStoreTask(taskId, "filmUri", Status.ACCEPTED);
        
        Task fetched = store.taskFor(taskId).get();
        
        assertEquals(task, fetched);
        
        assertEquals(task.destination(), fetched.destination());
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
    public void testUpdatingLastError() {
        long taskId = 1234l;
        Task task = createAndStoreTask(taskId, "filmUri", Status.ACCEPTED);
        
        store.save(task);
        
        store.updateWithLastError(taskId, "Error");
        Optional<Task> retrievedTask = store.taskFor(taskId);
        
        assertEquals("Error", retrievedTask.get().lastError().get());
    }
    
    @Test
    public void testLimitAndOffsetQueryCorrectlyTrimsReturnedSetOfTransactions() {
        Task t1 = createAndStoreTask(1234l, "", Status.ACCEPTED);
        Task t2 = createAndStoreTask(2345l, "", Status.ACCEPTED);
        Task t3 = createAndStoreTask(3456l, "", Status.ACCEPTED);
        
        Selection selection = selectionWithParams(100, 0);
        TaskQuery query = TaskQuery.builder(selection, PUBLISHER, TYPE).build();
        Iterable<Task> allTransactions = store.allTasks(query);
        
        assertEquals(ImmutableList.of(t1, t2, t3), ImmutableList.copyOf(allTransactions));
        
        selection = selectionWithParams(1, 1);
        query = TaskQuery.builder(selection, PUBLISHER, TYPE).build();
        allTransactions = store.allTasks(query);
        
        assertEquals(Iterables.getOnlyElement(allTransactions), t2);
    }
    
    @Test
    @Ignore
    public void testRetrievingTasksByStatusFiltersReturnedTasksCorrectly() {
        Task desired = createAndStoreTask(1234l, "", Status.NEW);
        createAndStoreTask(2345l, "", Status.FAILED);
        createAndStoreTask(3456l, "", Status.ACCEPTED);
        
        Iterable<Task> fetched = store.allTasks(YOUVIEW, Status.NEW);
        
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
        TaskQuery query = TaskQuery.builder(selection, PUBLISHER, TYPE)
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
        TaskQuery query = TaskQuery.builder(selection, PUBLISHER, TYPE)
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
        TaskQuery query = TaskQuery.builder(selection, PUBLISHER, TYPE)
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
        TaskQuery query = TaskQuery.builder(selection, PUBLISHER, TYPE)
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
        TaskQuery query = TaskQuery.builder(selection, PUBLISHER, TYPE)
                .withRemoteId("desired")
                .build();
        Iterable<Task> allTransactions = store.allTasks(query);
        
        assertEquals(desiredTask, Iterables.getOnlyElement(allTransactions));
    }
    
    @Test
    @Ignore
    public void testRemovalOfOldTasks() {
        DateTime removalDate = clock.now().minusDays(1);
        
        Task newTask = createAndStoreTask(1234l, "content", Status.ACCEPTED, removalDate.plusHours(1));
        Task oldTask = createAndStoreTask(2345l, "content", Status.ACCEPTED, removalDate.minusHours(1));
        
        Iterable<Task> allTasks = store.allTasks(YOUVIEW, Status.ACCEPTED);
        
        assertEquals(ImmutableSet.of(newTask, oldTask), ImmutableSet.copyOf(allTasks));
        
        store.removeBefore(removalDate);
        
        allTasks = store.allTasks(YOUVIEW, Status.ACCEPTED);
        
        assertEquals(ImmutableSet.of(newTask), ImmutableSet.copyOf(allTasks));
    }
    
    private Task createAndStoreTask(long taskId, String content, Status status) {
        return createAndStoreTask(taskId, content, status, DateTime.now());
    }
    
    private Task createAndStoreTask(long taskId, String content, Status status, DateTime created) {
        Task txn = createTask(
                taskId, 
                content,
                status,
                created
        );
        return store.save(txn);
    }

    private Selection selectionWithParams(int limit, int offset) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        Mockito.when(request.getParameter("limit")).thenReturn(String.valueOf(limit));
        Mockito.when(request.getParameter("offset")).thenReturn(String.valueOf(offset));
        
        return Selection.builder().build(request);
    }

    private Task createTask(long taskId, String contentUri,
            Status status, DateTime created) {
        return Task.builder()
                .withId(taskId)
                .withPublisher(PUBLISHER)
                .withCreated(created)
                .withDestination(createDestination(contentUri))
                .withPayload(DEFAULT_PAYLOAD)
                .withAction(Action.UPDATE)
                .withStatus(status)
                .build();
    }

    private Destination createDestination(String contentUri) {
        return new YouViewDestination(contentUri, TVAElementType.ITEM, "elementId");
    }
}
