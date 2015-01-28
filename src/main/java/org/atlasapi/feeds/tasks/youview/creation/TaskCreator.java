package org.atlasapi.feeds.tasks.youview.creation;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Content;

/**
 * This interface wraps up the creation of a {@link Task} from its given Atlas objects,
 * generating TVAnytime output, and producing a Task object containing this ouput.
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public interface TaskCreator {
    
    Task taskFor(String contentCrid, Content content, Action action);
    Task taskFor(String versionCrid, ItemAndVersion versionHierarchy, Action action);
    Task taskFor(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Action action);
    Task taskFor(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Action action);
}
