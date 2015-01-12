package org.atlasapi.feeds.youview.tasks.persistence;

import static org.junit.Assert.*;

import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Destination;
import org.atlasapi.feeds.youview.tasks.Payload;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.YouViewDestination;
import org.atlasapi.feeds.youview.tasks.persistence.IdSettingTaskStore;
import org.atlasapi.feeds.youview.tasks.persistence.MongoTaskStore;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.ids.MongoSequentialIdGenerator;
import org.joda.time.DateTime;
import org.junit.Test;

import com.metabroadcast.common.ids.IdGenerator;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class IdSettingTaskStoreTest {


    private static final DateTime TIME = new DateTime();
    private DatabasedMongo mongo = MongoTestHelper.anEmptyTestDatabase();
    private TaskStore delegate = new MongoTaskStore(mongo);
    private IdGenerator idGenerator = new MongoSequentialIdGenerator(mongo, "tasks");
    
    private final TaskStore store = new IdSettingTaskStore(delegate, idGenerator);
    
    @Test
    public void testSetsIdOnTaskIfTaskHasNoId() {
        Task task = createTaskWithId(null);
        
        Task saved = store.save(task);
        
        assertTrue("Id should be created if not set", saved.id() != null);
    }

    @Test
    public void testLeavesIdUnchangedIfTaskHasId() {
        Long taskId = 1234l;
        
        Task saved = store.save(createTaskWithId(taskId));

        assertEquals(taskId, saved.id());
    }

    private Task createTaskWithId(Long id) {
        return Task.builder()
                .withId(id)
                .withAction(Action.UPDATE)
                .withStatus(Status.NEW)
                .withPublisher(Publisher.METABROADCAST)
                .withPayload(new Payload("payload", TIME))
                .withDestination(createDestination())
                .build();
    }

    private Destination createDestination() {
        return new YouViewDestination("content", TVAElementType.ITEM, "elementId");
    }

}
