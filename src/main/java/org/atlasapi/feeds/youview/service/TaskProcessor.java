package org.atlasapi.feeds.youview.service;

import org.atlasapi.feeds.youview.tasks.Task;


public interface TaskProcessor {

    /**
     * Processes the main Task action, be that uploading, deleting or another
     * task.
     * 
     * @param task
     * @return a new Task updated with any changes resulting from the processing
     */
    Task process(Task task);
    
    /**
     * Checks the status of the action represented by the Task on the remote system.
     * 
     * @param task
     * @return a new Task updated with a response from the remote system
     */
    Task checkRemoteStatusOf(Task task);
}
