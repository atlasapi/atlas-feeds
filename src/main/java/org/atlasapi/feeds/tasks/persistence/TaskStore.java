package org.atlasapi.feeds.tasks.persistence;

import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.TaskQuery;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
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
    void updateWithResponse(Long taskId, Response response);
    
    /**
     * Updates a task with a {@link Payload}, containing the content (to be) uploaded to the 
     * remote site.
     * @param taskId
     * @param payload
     */
    void updateWithPayload(Long taskId, Payload payload);
    
    Optional<Task> taskFor(Long taskId);
    
    Iterable<Task> allTasks(TaskQuery query);
    
    /**
     * Fetches all tasks for a given DestinationType (~= feed destination, e.g. YouView, RadioPlayer)
     * and Status
     * @param type The destination type of feed desired
     * @param status the status to fetch 
     * @return
     */
    Iterable<Task> allTasks(DestinationType type, Status status);
    
    /**
     * Remove all Tasks from the store created before a certain date.  
     * @param removalDate
     */
    void removeBefore(DateTime removalDate);
}
