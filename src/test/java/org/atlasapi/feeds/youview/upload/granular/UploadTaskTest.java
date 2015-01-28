package org.atlasapi.feeds.youview.upload.granular;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.persistence.TaskStore;
import org.atlasapi.feeds.tasks.youview.processing.TaskProcessor;
import org.atlasapi.media.entity.Publisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class UploadTaskTest {
    
    private Clock CLOCK = new TimeMachine();

    private TaskStore taskStore = Mockito.mock(TaskStore.class);
    private TaskProcessor processor = Mockito.mock(TaskProcessor.class);
    private Task newTask = createTask(Status.NEW);
    private Task nonNewTask = createTask(Status.ACCEPTED);
    
    private final UpdateTask uploadTask = new UpdateTask(taskStore, processor);
    
    @Before
    public void setup() {
        when(taskStore.allTasks(eq(Status.NEW))).thenReturn(ImmutableSet.of(newTask));
        when(taskStore.allTasks(not(eq(Status.NEW)))).thenReturn(ImmutableSet.of(nonNewTask));
    }
    
    @Test
    public void testIdentifiesCorrectTasksFromStore() {
        uploadTask.run();
        
        verify(taskStore, only()).allTasks(Status.NEW);
        verify(processor, only()).process(newTask);
    }

    private Task createTask(Status status) {
        return Task.builder()
                .withId(1234l)
                .withPublisher(Publisher.METABROADCAST)
                .withContent("content")
                .withElementType(TVAElementType.ITEM)
                .withElementId("elementId")
                .withPayload(createPayload())
                .withAction(Action.UPDATE)
                .withStatus(status)
                .build();
    }
    
    private Payload createPayload() {
        return new Payload("payload", CLOCK.now());
    }
}
