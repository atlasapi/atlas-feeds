package org.atlasapi.feeds.tasks.youview.creation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.feeds.youview.persistence.HashType;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class DeltaTaskCreationTaskTest {

    private static final Publisher PUBLISHER = Publisher.BBC;

    private DeltaTaskCreationTask task;

    @Mock
    private YouViewLastUpdatedStore lastUpdatedStore;

    @Mock
    private ContentHierarchyExpander hierarchyExpander;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TaskStore taskStore;

    @Mock
    private TaskCreator taskCreator;

    @Mock
    private PayloadCreator payloadCreator;

    @Mock
    private UpdateTask updateTask;

    @Mock
    private YouViewContentResolver contentResolver;

    @Mock
    private YouViewPayloadHashStore payloadHashStore;

    @Before
    public void setUp() {
        task = new DeltaTaskCreationTask(
            lastUpdatedStore,
            PUBLISHER,
            hierarchyExpander,
            idGenerator,
            taskStore,
            taskCreator,
            payloadCreator,
            updateTask,
            contentResolver,
            payloadHashStore
        );
    }

    @Test
    public void createsUpdateTaskForItemsIfHashDoesNotMatch() throws PayloadGenerationException {
        DateTime updatedSince = DateTime.now().minusDays(1);
        DateTime created = DateTime.now();
        String contentCrid = "foobar crid";
        String payloadBody = "foobar body";
        long taskId = 42L;
        Action action = Action.UPDATE;
        Payload payload = new Payload(payloadBody, created);

        Task.Builder taskBuilder = Task.builder()
                .withAction(action)
                .withDestination(new YouViewDestination("", TVAElementType.ITEM, ""))
                .withCreated(created)
                .withStatus(Status.NEW)
                .withPublisher(PUBLISHER);
        Task withoutId = taskBuilder.build();
        Task withId = taskBuilder.withId(taskId).build();

        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(
                Optional.of(updatedSince));

        Content content = mock(Content.class);
        when(contentResolver.updatedSince(updatedSince))
                .thenReturn(Lists.newArrayList(content)
                .iterator());
        when(content.isActivelyPublished()).thenReturn(true);
        when(idGenerator.generateContentCrid(content)).thenReturn(contentCrid);
        when(payloadHashStore.getHash(HashType.CONTENT, contentCrid))
                .thenReturn(Optional.of(payload.hash() + "nope"));
        when(taskCreator.taskFor(contentCrid, content, action)).thenReturn(withoutId);
        when(taskStore.save(withoutId)).thenReturn(withId);
        when(payloadCreator.payloadFrom(contentCrid, content)).thenReturn(payload);

        task.runTask();

        verify(taskStore).updateWithPayload(taskId, payload);
        verify(payloadHashStore).saveHash(HashType.CONTENT, contentCrid, payload.hash());
    }

    @Test
    public void createUpdateTaskForItemsIfHashNotFound() throws PayloadGenerationException {
        DateTime updatedSince = DateTime.now().minusDays(1);
        DateTime created = DateTime.now();
        String contentCrid = "foobar crid";
        String payloadBody = "foobar body";
        long taskId = 42L;
        Action action = Action.UPDATE;
        Payload payload = new Payload(payloadBody, created);

        Task.Builder taskBuilder = Task.builder()
                .withAction(action)
                .withDestination(new YouViewDestination("", TVAElementType.ITEM, ""))
                .withCreated(created)
                .withStatus(Status.NEW)
                .withPublisher(PUBLISHER);
        Task withoutId = taskBuilder.build();
        Task withId = taskBuilder.withId(taskId).build();

        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(
                Optional.of(updatedSince));

        Content content = mock(Content.class);
        when(contentResolver.updatedSince(updatedSince))
                .thenReturn(Lists.newArrayList(content)
                        .iterator());
        when(content.isActivelyPublished()).thenReturn(true);
        when(idGenerator.generateContentCrid(content)).thenReturn(contentCrid);
        when(payloadHashStore.getHash(HashType.CONTENT, contentCrid))
                .thenReturn(Optional.<String>absent());
        when(taskCreator.taskFor(contentCrid, content, action)).thenReturn(withoutId);
        when(taskStore.save(withoutId)).thenReturn(withId);
        when(payloadCreator.payloadFrom(contentCrid, content)).thenReturn(payload);

        task.runTask();

        verify(taskStore).updateWithPayload(taskId, payload);
        verify(payloadHashStore).saveHash(HashType.CONTENT, contentCrid, payload.hash());
    }

    @Test
    public void doesNotCreateTaskIfPayloadHashesMatch() throws PayloadGenerationException {
        DateTime updatedSince = DateTime.now().minusDays(1);
        DateTime created = DateTime.now();
        String contentCrid = "foobar crid";
        String payloadBody = "foobar body";
        long taskId = 42L;
        Action action = Action.UPDATE;
        Payload payload = new Payload(payloadBody, created);

        Task.Builder taskBuilder = Task.builder()
                .withAction(action)
                .withDestination(new YouViewDestination("", TVAElementType.ITEM, ""))
                .withCreated(created)
                .withStatus(Status.NEW)
                .withPublisher(PUBLISHER);
        Task withoutId = taskBuilder.build();

        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(
                Optional.of(updatedSince));

        Content content = mock(Content.class);
        when(contentResolver.updatedSince(updatedSince))
                .thenReturn(Lists.newArrayList(content)
                .iterator());
        when(content.isActivelyPublished()).thenReturn(true);
        when(idGenerator.generateContentCrid(content)).thenReturn(contentCrid);
        when(payloadHashStore.getHash(HashType.CONTENT, contentCrid))
                .thenReturn(Optional.of(payload.hash()));
        when(payloadCreator.payloadFrom(contentCrid, content)).thenReturn(payload);

        task.runTask();

        verify(taskCreator, never()).taskFor(contentCrid, content, action);
        verify(taskStore, never()).save(withoutId);
        verify(taskStore, never()).updateWithPayload(taskId, payload);
        verify(payloadHashStore, never()).saveHash(HashType.CONTENT, contentCrid, payload.hash());
    }

    @Test
    public void createsDeleteTaskForCancelledItems() throws PayloadGenerationException {
        DateTime updatedSince = DateTime.now().minusDays(1);
        DateTime created = DateTime.now();
        String contentCrid = "foobar crid";
        long taskId = 42L;
        Action action = Action.DELETE;
        Task.Builder taskBuilder = Task.builder()
                .withAction(action)
                .withDestination(new YouViewDestination("", TVAElementType.ITEM, ""))
                .withCreated(created)
                .withStatus(Status.ACCEPTED)
                .withPublisher(PUBLISHER);
        Task withoutId = taskBuilder.build();
        Task withId = taskBuilder.withId(taskId).build();

        when(lastUpdatedStore.getLastUpdated(PUBLISHER)).thenReturn(
                Optional.of(updatedSince));

        Content content = mock(Content.class);
        when(contentResolver.updatedSince(updatedSince))
                .thenReturn(Lists.newArrayList(content)
                        .iterator());
        when(content.isActivelyPublished()).thenReturn(false);
        when(idGenerator.generateContentCrid(content)).thenReturn(contentCrid);
        when(taskCreator.taskFor(contentCrid, content, action)).thenReturn(withoutId);
        when(taskStore.save(withoutId)).thenReturn(withId);

        task.runTask();
    }
}