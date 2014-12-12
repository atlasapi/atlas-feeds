package org.atlasapi.feeds.youview.tasks.creation;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Content;

/**
 * This interface wraps up the creation of a task from its given Atlas objects,
 * generating TVAnytime output, and producing a Task object containing this ouput.
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public interface TaskCreator {
    
    Task create(String contentCrid, Content content, Action action);
    Task create(String versionCrid, ItemAndVersion versionHierarchy, Action action);
    Task create(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Action action);
    Task create(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Action action);
}
