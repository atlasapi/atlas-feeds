package org.atlasapi.feeds.youview.upload.granular;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Content;


public interface GranularYouViewService {

    void uploadContent(Content content);
    void uploadVersion(ItemAndVersion versionHierarchy, String versionCrid);
    void uploadBroadcast(ItemBroadcastHierarchy broadcastHierarchy, String broadcastImi);
    void uploadOnDemand(ItemOnDemandHierarchy onDemandHierarchy, String onDemandImi);
    void sendDeleteFor(Content content, TVAElementType type, String elementId);
    void checkRemoteStatusOf(Task task);
}
