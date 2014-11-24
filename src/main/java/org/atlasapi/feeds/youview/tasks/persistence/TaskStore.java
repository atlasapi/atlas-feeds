package org.atlasapi.feeds.youview.tasks.persistence;

import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.TaskQuery;

import com.google.common.base.Optional;


public interface TaskStore {
    
    Task save(Task task);
    
    /**
     * Updates the record for a given task with a new status. 
     * Returns true if the update occurs, and false otherwise.
     * @param taskId the id of the task to be updated
     * @param response the desired new status
     * @return
     */
    boolean updateWithStatus(Long taskId, Status status);
    
    boolean updateWithRemoteId(Long taskId, Status status, String remoteId);
    
    /**
     * Updates the record for a given task with a response from the remote site. 
     * Returns true if the update occurs, and false otherwise.
     * @param taskId the id of the task to be updated
     * @param response the response from the remote site
     * @return
     */
    boolean updateWithResponse(Long taskId, Response response);
    
    Optional<Task> taskFor(Long taskId);
    
    Iterable<Task> allTasks(TaskQuery query);
    
    Iterable<Task> allTasks(Status status);
}
