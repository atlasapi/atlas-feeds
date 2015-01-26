package org.atlasapi.feeds.youview.tasks.processing;

import org.atlasapi.feeds.youview.tasks.Task;


public interface TaskProcessor {

    /**
     * Processes the main Task action, be that uploading or deleting
     * 
     * @param task
     * @return UpdateProgress representing the progress through the elements processed
     */
    void process(Task task);
    
    /**
     * Checks the status of the action represented by the Task on the remote system.
     * 
     * @param task
     * @return UpdateProgress representing the progress through the elements processed
     */
    void checkRemoteStatusOf(Task task);
}
