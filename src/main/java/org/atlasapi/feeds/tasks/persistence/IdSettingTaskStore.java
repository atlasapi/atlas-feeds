package org.atlasapi.feeds.tasks.persistence;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.TaskQuery;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
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
    public void updateWithPayload(Long taskId, Payload payload) {
        delegate.updateWithPayload(taskId, payload);
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
    public Iterable<Task> allTasks(DestinationType type, Status status) {
        return delegate.allTasks(type, status);
    }

    @Override
    public void removeBefore(DateTime removalDate) {
        delegate.removeBefore(removalDate);
    }

    @Override
    public void updateWithLastError(Long taskId, String lastError) {
        delegate.updateWithLastError(taskId, lastError);
    }
}
