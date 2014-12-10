package org.atlasapi.feeds.youview.nitro;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;


public interface BbcServiceIdResolver {

    String resolveSId(Broadcast broadcast);
    String resolveSId(Content content);
    String resolveMasterBrandId(Content content);
}
