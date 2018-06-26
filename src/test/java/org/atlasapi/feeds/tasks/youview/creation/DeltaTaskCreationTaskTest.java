package org.atlasapi.feeds.tasks.youview.creation;

import com.google.api.client.util.Sets;
import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.DeleteTask;
import org.atlasapi.feeds.tasks.youview.processing.UpdateTask;
import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.feeds.youview.persistence.HashType;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.feeds.youview.persistence.YouViewPayloadHashStore;
import org.atlasapi.feeds.youview.resolution.YouViewContentResolver;
import org.atlasapi.media.channel.ChannelQuery;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.atlasapi.persistence.lookup.entry.LookupEntryStore;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeltaTaskCreationTaskTest {

    private static final Publisher PUBLISHER = Publisher.BBC_NITRO;

    private DeltaTaskCreationTask task;

    @Mock private YouViewLastUpdatedStore lastUpdatedStore;
    @Mock private ContentHierarchyExpander hierarchyExpander;
    @Mock private IdGenerator idGenerator;
    @Mock private TaskStore taskStore;
    @Mock private TaskCreator taskCreator;
    @Mock private PayloadCreator payloadCreator;
    @Mock private UpdateTask updateTask;
    @Mock private DeleteTask deleteTask;
    @Mock private YouViewContentResolver contentResolver;
    @Mock private YouViewPayloadHashStore payloadHashStore;
    @Mock private ChannelResolver channelResolver;
    @Mock private KnownTypeQueryExecutor mergingResolver;
    @Mock private LookupEntryStore lookupEntryStore;

    @Before
    public void setUp() {
        System.setProperty("SERVEROPTS_REPID_SERVICE_HOST", "no_host.com:80");
        System.setProperty("SERVEROPTS_APPLICATIONS_CLIENT_HOST", "http://www.nohost.com");
        System.setProperty("SERVEROPTS_YOUVIEW_UPLOAD_UNBOX_EQUIVAPPID", "no_app");
        System.setProperty("SERVEROPTS_YOUVIEW_UPLOAD_NITRO_EQUIVAPPID", "no_app");

        task = new DeltaTaskCreationTask(
                lastUpdatedStore,
                PUBLISHER,
                hierarchyExpander,
                idGenerator,
                taskStore,
                taskCreator,
                payloadCreator,
                updateTask,
                deleteTask,
                contentResolver,
                payloadHashStore,
                channelResolver,
                mergingResolver,
                lookupEntryStore
        );
        when(channelResolver.allChannels(any(ChannelQuery.class))).thenReturn(ImmutableList.of());
    }

    @Test
    @Ignore
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
        when(contentResolver.updatedSince(updatedSince.minus(DeltaTaskCreationTask.UPDATE_WINDOW_GRACE_PERIOD)))
                .thenReturn(Lists.newArrayList(content)
                .iterator());
        when(content.isActivelyPublished()).thenReturn(true);
        when(idGenerator.generateContentCrid(content)).thenReturn(contentCrid);
        when(payloadHashStore.getHash(HashType.CONTENT, contentCrid))
                .thenReturn(java.util.Optional.of(payload.hash() + "nope"));
        when(taskCreator.taskFor(contentCrid, content, payload, action)).thenReturn(withoutId);
        when(taskStore.save(withoutId)).thenReturn(withId);
        when(payloadCreator.payloadFrom(contentCrid, content)).thenReturn(payload);
        when(lookupEntryStore.updatedSince(any(Publisher.class), any(DateTime.class))).thenReturn(Sets.newHashSet());

        task.runTask();

        verify(payloadHashStore).saveHash(HashType.CONTENT, contentCrid, payload.hash());
    }

    @Test
    @Ignore
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
        when(contentResolver.updatedSince(updatedSince.minus(DeltaTaskCreationTask.UPDATE_WINDOW_GRACE_PERIOD)))
                .thenReturn(Lists.newArrayList(content)
                        .iterator());
        when(content.isActivelyPublished()).thenReturn(true);
        when(idGenerator.generateContentCrid(content)).thenReturn(contentCrid);
        when(payloadHashStore.getHash(HashType.CONTENT, contentCrid))
                .thenReturn(java.util.Optional.<String>empty());
        when(taskCreator.taskFor(contentCrid, content, payload, action)).thenReturn(withoutId);
        when(taskStore.save(withoutId)).thenReturn(withId);
        when(payloadCreator.payloadFrom(contentCrid, content)).thenReturn(payload);

        task.runTask();

        verify(payloadHashStore).saveHash(HashType.CONTENT, contentCrid, payload.hash());
    }

    @Test
    @Ignore
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
        when(contentResolver.updatedSince(updatedSince.minus(DeltaTaskCreationTask.UPDATE_WINDOW_GRACE_PERIOD)))
                .thenReturn(Lists.newArrayList(content)
                .iterator());
        when(content.isActivelyPublished()).thenReturn(true);
        when(idGenerator.generateContentCrid(content)).thenReturn(contentCrid);
        when(payloadHashStore.getHash(HashType.CONTENT, contentCrid))
                .thenReturn(java.util.Optional.of(payload.hash()));
        when(payloadCreator.payloadFrom(contentCrid, content)).thenReturn(payload);

        task.runTask();

        verify(taskCreator, never()).taskFor(contentCrid, content, payload, action);
        verify(taskStore, never()).save(withoutId);
        verify(payloadHashStore, never()).saveHash(HashType.CONTENT, contentCrid, payload.hash());
    }

    @Test
    @Ignore
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
        when(contentResolver.updatedSince(updatedSince.minus(DeltaTaskCreationTask.UPDATE_WINDOW_GRACE_PERIOD)))
                .thenReturn(Lists.newArrayList(content)
                        .iterator());
        when(content.isActivelyPublished()).thenReturn(false);
        when(idGenerator.generateContentCrid(content)).thenReturn(contentCrid);
        when(taskCreator.taskFor(contentCrid, content, action, Status.NEW)).thenReturn(withoutId);
        when(taskStore.save(withoutId)).thenReturn(withId);

        task.runTask();
    }

    @Test
    @Ignore
    public void createsDeleteTaskForUnavailableOnDemands() throws PayloadGenerationException {
        DateTime updatedSince = DateTime.now().minusDays(1);
        DateTime created = DateTime.now();
        String contentCrid = "foobar crid";
        String onDemandImi = "ondemand crid";
        String contentPayloadBody = "foobar body";
        String onDemandPayloadBody = "ondemand body";
        long taskId = 42L;
        Action action = Action.UPDATE;

        Payload contentPayload = new Payload(contentPayloadBody, created);
        Payload onDemandPayload = new Payload(onDemandPayloadBody, created);

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

        Item content = mock(Item.class);

        Location location = mock(Location.class);
        Encoding encoding = mock(Encoding.class);
        Version version = mock(Version.class);

        when(content.getVersions()).thenReturn(ImmutableSet.of(version));
        when(version.getManifestedAs()).thenReturn(ImmutableSet.of(encoding));
        when(encoding.getAvailableAt()).thenReturn(ImmutableSet.of(location));
        when(location.getAvailable()).thenReturn(false);

        when(contentResolver.updatedSince(updatedSince.minus(DeltaTaskCreationTask.UPDATE_WINDOW_GRACE_PERIOD)))
                .thenReturn(Lists.newArrayList((Content) content).iterator());
        when(content.isActivelyPublished()).thenReturn(true);

        // ondemand upload
        when(idGenerator.generateOnDemandImi(content, version, encoding,  any()))
                .thenReturn(onDemandImi);
        when(payloadHashStore.getHash(HashType.ON_DEMAND, onDemandImi))
                .thenReturn(java.util.Optional.of(onDemandPayload.hash() + "nope"));

        when(payloadCreator.payloadFrom(eq(onDemandImi), any(ItemOnDemandHierarchy.class)))
                .thenReturn(onDemandPayload);
        when(taskCreator.taskFor(
                eq(onDemandImi),
                any(ItemOnDemandHierarchy.class),
                eq(onDemandPayload),
                eq(Action.DELETE)
        )).thenReturn(withoutId);

        // content upload
        when(idGenerator.generateContentCrid(content)).thenReturn(contentCrid);
        when(payloadHashStore.getHash(HashType.CONTENT, contentCrid))
                .thenReturn(java.util.Optional.of(contentPayload.hash() + "nope"));

        when(payloadCreator.payloadFrom(contentCrid, content)).thenReturn(contentPayload);
        when(taskCreator.taskFor(contentCrid, content, contentPayload, action)).thenReturn(withoutId);

        // these go last because arg matching order is important
        when(taskStore.save(withoutId)).thenReturn(withId);
        when(taskStore.save(any(Task.class))).thenReturn(withId);

        task.runTask();

        verify(payloadHashStore).saveHash(HashType.CONTENT, contentCrid, contentPayload.hash());
    }
}