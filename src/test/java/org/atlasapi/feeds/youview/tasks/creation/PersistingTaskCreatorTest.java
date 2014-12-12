package org.atlasapi.feeds.youview.tasks.creation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Payload;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class PersistingTaskCreatorTest {

    private static final Item ITEM = mock(Item.class);
    private static final Version VERSION = mock(Version.class);
    private static final Broadcast BROADCAST = mock(Broadcast.class);
    private static final Encoding ENCODING = mock(Encoding.class);
    private static final Location LOCATION = mock(Location.class);
    private static final Clock CLOCK = new TimeMachine();
    private static final Task TASK = createTask();
    
    private TaskCreator delegate = mock(TaskCreator.class);
    private TaskStore taskStore = mock(TaskStore.class);
    
    private final TaskCreator creator = new PersistingTaskCreator(delegate, taskStore);
    
    @Before
    public void setup() {
        when(delegate.create(anyString(), any(Content.class), any(Action.class))).thenReturn(TASK);
        when(delegate.create(anyString(), any(ItemAndVersion.class), any(Action.class))).thenReturn(TASK);
        when(delegate.create(anyString(), any(ItemBroadcastHierarchy.class), any(Action.class))).thenReturn(TASK);
        when(delegate.create(anyString(), any(ItemOnDemandHierarchy.class), any(Action.class))).thenReturn(TASK);
        
        // task store echos back any task passed to the save method
        when(taskStore.save(any(Task.class))).thenAnswer(new Answer<Task>() {
            @Override
            public Task answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return (Task) args[0];
            }
        });
    }

    @Test
    public void testCreationOfContentTaskPersistsTask() {
        Action update = Action.UPDATE;
        String contentCrid = "content";
        
        Task createdTask = creator.create(contentCrid, ITEM, update);
        
        assertEquals(TASK, createdTask);
        verify(delegate).create(contentCrid, ITEM, update);
        verify(taskStore).save(TASK);
    }

    @Test
    public void testCreationOfVersionTaskPersistsTask() {
        ItemAndVersion versionHierarchy = new ItemAndVersion(ITEM, VERSION);
        Action update = Action.UPDATE;
        String versionCrid = "version";
        
        Task createdTask = creator.create(versionCrid, versionHierarchy, update);
        
        assertEquals(TASK, createdTask);
        verify(delegate).create(versionCrid, versionHierarchy, update);
        verify(taskStore).save(TASK);
    }

    @Test
    public void testCreationOfBroadcastTaskPersistsTask() {
        ItemBroadcastHierarchy broadcastHierarchy = new ItemBroadcastHierarchy(ITEM, VERSION, BROADCAST, "serviceId");
        Action update = Action.UPDATE;
        String broadcastImi = "broadcast";
        
        Task createdTask = creator.create(broadcastImi, broadcastHierarchy, update);
        
        assertEquals(TASK, createdTask);
        verify(delegate).create(broadcastImi, broadcastHierarchy, update);
        verify(taskStore).save(TASK);
    }

    @Test
    public void testCreationOfOnDemandTaskPersistsTask() {
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(ITEM, VERSION, ENCODING, LOCATION);
        Action update = Action.UPDATE;
        String onDemandImi = "onDemand";
        
        Task createdTask = creator.create(onDemandImi, onDemandHierarchy, update);
        
        assertEquals(TASK, createdTask);
        verify(delegate).create(onDemandImi, onDemandHierarchy, update);
        verify(taskStore).save(TASK);
    }

    private static Task createTask() {
        return Task.builder()
                .withId(1234l)
                .withPublisher(Publisher.METABROADCAST)
                .withContent("content")
                .withElementType(TVAElementType.ITEM)
                .withElementId("elementId")
                .withPayload(createPayload())
                .withAction(Action.UPDATE)
                .withStatus(Status.ACCEPTED)
                .build();
    }
    
    private static Payload createPayload() {
        return new Payload("payload", CLOCK.now());
    }
}
