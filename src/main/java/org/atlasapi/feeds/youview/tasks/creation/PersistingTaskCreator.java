package org.atlasapi.feeds.youview.tasks.creation;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.persistence.TaskStore;
import org.atlasapi.media.entity.Content;


public class PersistingTaskCreator implements TaskCreator {

    private final TaskCreator delegate;
    private final TaskStore taskStore;
    
    public PersistingTaskCreator(TaskCreator delegate, TaskStore taskStore) {
        this.delegate = checkNotNull(delegate);
        this.taskStore = checkNotNull(taskStore);
    }

    @Override
    public Task create(String contentCrid, Content content, Action action) throws TaskCreationException {
        return taskStore.save(delegate.create(contentCrid, content, action));
    }

    @Override
    public Task create(String versionCrid, ItemAndVersion versionHierarchy, Action action) throws TaskCreationException {
        return taskStore.save(delegate.create(versionCrid, versionHierarchy, action));
    }

    @Override
    public Task create(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Action action) throws TaskCreationException {
        return taskStore.save(delegate.create(broadcastImi, broadcastHierarchy, action));
    }

    @Override
    public Task create(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Action action) throws TaskCreationException {
        return taskStore.save(delegate.create(onDemandImi, onDemandHierarchy, action));
    }

}
