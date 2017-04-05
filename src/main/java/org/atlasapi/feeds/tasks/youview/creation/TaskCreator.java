package org.atlasapi.feeds.tasks.youview.creation;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Content;

/**
 * This interface wraps up the creation of a {@link Task} from its given Atlas objects,
 * generating TVAnytime output, and producing a Task object containing this ouput.
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public interface TaskCreator {

    Task deleteFor(String contentCrid, Content content);
    Task deleteFor(String versionCrid, ItemAndVersion versionHierarchy);
    Task deleteFor(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy);
    Task deleteFor(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy);
    Task deleteFor(String channelCrid, Channel channel);

    Task taskFor(String contentCrid, Content content, Action action, Status status);
    Task taskFor(String versionCrid, ItemAndVersion versionHierarchy, Action action, Status status);
    Task taskFor(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Action action, Status status);
    Task taskFor(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Action action, Status status);
    Task taskFor(String channelCrid, Channel channel, Action action, Status status);

    Task taskFor(String contentCrid, Content content, Payload payload, Action action);
    Task taskFor(String versionCrid, ItemAndVersion versionHierarchy, Payload payload, Action action);
    Task taskFor(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Payload payload, Action action);
    Task taskFor(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Payload payload, Action action);
    Task taskFor(String channelCrid, Channel channel, Payload payload, Action action);
}
