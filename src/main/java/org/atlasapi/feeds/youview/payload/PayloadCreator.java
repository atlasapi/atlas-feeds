package org.atlasapi.feeds.youview.payload;

import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Content;

import com.google.common.base.Optional;


public interface PayloadCreator {

    Payload payloadFrom(Channel channel) throws PayloadGenerationException;

    Payload payloadFrom(String contentCrid, Content content) throws PayloadGenerationException;
    
    Payload payloadFrom(String versionCrid, ItemAndVersion versionHierarchy) throws PayloadGenerationException;
    
    /**
     * Given a broadcast IMI and Atlas broadcast hierarchy, generates output, and places it into a {@link Payload}.
     * @param broadcastImi unique generated identifier for the broadcast
     * @param broadcastHierarchy
     * @return Optional.of the generated payload, unless the Broadcast lacks certain requiredidentifiers, in which 
     * case Optional.absent is returned
     * @throws PayloadGenerationException if an error occurs during generation of the output feed
     */
    Optional<Payload> payloadFrom(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy) throws PayloadGenerationException;
    
    Payload payloadFrom(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy) throws PayloadGenerationException;
}
