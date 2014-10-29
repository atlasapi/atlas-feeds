package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Broadcast;


public interface BroadcastIdGenerator {

    String generate(Broadcast broadcast); 
}
