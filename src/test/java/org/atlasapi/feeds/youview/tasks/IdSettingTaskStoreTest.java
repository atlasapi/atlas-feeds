package org.atlasapi.feeds.youview.tasks;

import static org.junit.Assert.*;

import org.atlasapi.feeds.youview.tasks.persistence.IdSettingTaskStore;
import org.atlasapi.feeds.youview.tasks.persistence.MongoTaskStore;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.ids.MongoSequentialIdGenerator;
import org.junit.Test;

import com.metabroadcast.common.ids.IdGenerator;
import com.metabroadcast.common.persistence.MongoTestHelper;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;


public class IdSettingTaskStoreTest {


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
                .withContent("content")
                .build();
    }

}
