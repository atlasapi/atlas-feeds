package org.atlasapi.feeds.youview;

import java.util.UUID;

import org.atlasapi.media.entity.Broadcast;


public class UUIDBasedBroadcastIdGenerator implements BroadcastIdGenerator {

    @Override
    public String generate(Broadcast broadcast) {
        // TODO not reproducible
        return UUID.randomUUID().toString();
    }

}
