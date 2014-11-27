package org.atlasapi.feeds.youview.tasks.persistence;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.TaskQuery;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.metabroadcast.common.ids.IdGenerator;


public class IdSettingTaskStore implements TaskStore {

    private final IdGenerator idGenerator;
    private final TaskStore delegate;
    
    public IdSettingTaskStore(TaskStore delegate, IdGenerator idGenerator) {
        this.delegate = checkNotNull(delegate);
        this.idGenerator = checkNotNull(idGenerator);
    }

    @Override
    public Task save(Task task) {
        return delegate.save(generateId(task));
    }

    private Task generateId(Task task) {
        if (task.id() == null) {
            task.setId(idGenerator.generateRaw());
        }
        return task;
    }

    @Override
    public void updateWithStatus(Long taskId, Status status) {
        delegate.updateWithStatus(taskId, status);
    }

    @Override
    public void updateWithRemoteId(Long taskId, Status status, String remoteId, DateTime uploadTime) {
        delegate.updateWithRemoteId(taskId, status, remoteId, uploadTime);
    }

    @Override
    public void updateWithResponse(Long taskId, Response response) {
        delegate.updateWithResponse(taskId, response);
    }

    @Override
    public Optional<Task> taskFor(Long taskId) {
        return delegate.taskFor(taskId);
    }

    @Override
    public Iterable<Task> allTasks(TaskQuery query) {
        return delegate.allTasks(query);
    }

    @Override
    public Iterable<Task> allTasks(Status status) {
        return delegate.allTasks(status);
    }

}
