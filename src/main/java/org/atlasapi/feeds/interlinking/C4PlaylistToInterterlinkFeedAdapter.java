package org.atlasapi.feeds.interlinking;

import org.atlasapi.media.entity.Broadcast;

public class C4PlaylistToInterterlinkFeedAdapter extends PlaylistToInterlinkFeedAdapter implements PlaylistToInterlinkFeed {
    
    private static final String BROADCAST_ID_PREFIX = "tag:";
    
    @Override
    protected InterlinkBroadcast fromBroadcast(Broadcast broadcast) {
        String id = null;
        for (String alias : broadcast.getAliases()) {
            if (alias.startsWith(BROADCAST_ID_PREFIX) || alias.startsWith("urn:"+BROADCAST_ID_PREFIX)) {
                id = alias;
                break;
            }
        }

        if (id != null) {
            return new InterlinkBroadcast(id).withDuration(toDuration(broadcast.getBroadcastDuration())).withBroadcastStart(broadcast.getTransmissionTime());
        }
        return null;
    }
}
