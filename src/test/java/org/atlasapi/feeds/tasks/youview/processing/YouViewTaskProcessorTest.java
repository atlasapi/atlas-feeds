package org.atlasapi.feeds.tasks.youview.processing;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
import org.atlasapi.feeds.youview.client.ResultHandler;
import org.atlasapi.feeds.youview.client.YouViewClient;
import org.atlasapi.feeds.youview.client.YouViewResult;
import org.atlasapi.feeds.youview.revocation.RevokedContentStore;
import org.atlasapi.reporting.telescope.TelescopeProxy;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class YouViewTaskProcessorTest {

    private YouViewTaskProcessor youViewTaskProcessor;

    @Mock private YouViewClient client;
    @Mock private ResultHandler resultHandler;
    @Mock private RevokedContentStore revocationStore;
    @Mock private TaskStore taskStore;
    @Mock private TelescopeProxy telescope;

    @Before
    public void setUp() {
        youViewTaskProcessor = new YouViewTaskProcessor(client, resultHandler, revocationStore, taskStore);
    }

    @Test
    public void updateTasksGetUploaded() {
        String contentUri = "content uri";
        Payload payload = new Payload("", DateTime.now());

        Task task = mock(Task.class);
        YouViewResult result = mock(YouViewResult.class);

        when(task.action()).thenReturn(Action.UPDATE);
        when(task.payload()).thenReturn(Optional.of(payload));
        when(task.destination()).thenReturn(new YouViewDestination(contentUri, TVAElementType.ITEM, ""));
        when(revocationStore.isRevoked(contentUri)).thenReturn(Boolean.FALSE);
        when(client.upload(payload)).thenReturn(result);

        youViewTaskProcessor.process(task, telescope);

        verify(resultHandler).handleTransactionResult(task, result, telescope);
    }

    @Test
    public void tasksWithRevokedContentGetStatusUpdated() {
        String contentUri = "content uri";
        Payload payload = new Payload("", DateTime.now());
        Long taskId = 42L;

        Task task = mock(Task.class);

        when(task.action()).thenReturn(Action.UPDATE);
        when(task.payload()).thenReturn(Optional.of(payload));
        when(task.destination()).thenReturn(new YouViewDestination(contentUri, TVAElementType.ITEM, ""));
        when(task.id()).thenReturn(taskId);

        when(revocationStore.isRevoked(contentUri)).thenReturn(Boolean.TRUE);

        youViewTaskProcessor.process(task, telescope);

        verify(taskStore).updateWithStatus(taskId, Status.FAILED);
        verify(client, never()).upload(payload);
        verify(resultHandler, never()).handleTransactionResult(eq(task), any(YouViewResult.class), eq(telescope));
    }
}
