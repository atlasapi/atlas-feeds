package org.atlasapi.feeds.youview.nitro;

import org.atlasapi.media.entity.Broadcast;


public interface BbcServiceIdResolver {

    String resolveSId(Broadcast broadcast);
}
