package org.atlasapi.feeds.youview.tasks.persistence;

import org.atlasapi.feeds.youview.tasks.Response;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.tasks.TaskQuery;
import org.joda.time.DateTime;

import com.google.common.base.Optional;


public interface TaskStore {
    
    Task save(Task task);
    
    /**
     * Updates the record for a given task with a new status. 
     * @param taskId the id of the task to be updated
     * @param status the desired new status
     */
    void updateWithStatus(Long taskId, Status status);
    
    /**
     * Updates a given task with an ID provided by the remote system, along with
     * an updated status and a time of upload
     * @param taskId
     * @param status
     * @param remoteId
     * @param uploadTime
     */
    void updateWithRemoteId(Long taskId, Status status, String remoteId, DateTime uploadTime);
    
    /**
     * Updates the record for a given task with a response from the remote site. 
     * @param taskId the id of the task to be updated
     * @param response the response from the remote site
     */
    void  updateWithResponse(Long taskId, Response response);
    
    Optional<Task> taskFor(Long taskId);
    
    Iterable<Task> allTasks(TaskQuery query);
    
    Iterable<Task> allTasks(Status status);
}
