package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;

import com.google.common.base.Optional;

public interface ServiceIdResolver {

    Optional<String> resolveSId(Broadcast broadcast);
    Optional<String> resolveSId(Content content);
    Optional<String> resolveMasterBrandId(Content content);
}
