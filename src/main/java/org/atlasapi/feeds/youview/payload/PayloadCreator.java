package org.atlasapi.feeds.youview.payload;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.tasks.Payload;
import org.atlasapi.media.entity.Content;


public interface PayloadCreator {

    Payload payloadFrom(String contentCrid, Content content) throws PayloadGenerationException;
    Payload payloadFrom(String versionCrid, ItemAndVersion versionHierarchy) throws PayloadGenerationException;
    Payload payloadFrom(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy) throws PayloadGenerationException;
    Payload payloadFrom(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy) throws PayloadGenerationException;
}
