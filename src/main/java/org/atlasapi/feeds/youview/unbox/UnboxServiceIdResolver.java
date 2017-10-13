package org.atlasapi.feeds.youview.unbox;

import org.atlasapi.feeds.youview.ServiceIdResolver;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;

import com.google.common.base.Optional;

public final class UnboxServiceIdResolver implements ServiceIdResolver {

    public UnboxServiceIdResolver() {
    }

    @Override
    public Optional<String> resolveSId(Broadcast broadcast) {
        throw new UnsupportedOperationException("Unbox cannot resolve serviceIds, because its "
                                                + "content is not expected to be broadcasted.");
    }

    @Override
    public Optional<String> resolveSId(Content content) {
        throw new UnsupportedOperationException("Unbox cannot resolve serviceIds because its content "
                                                + "is not expected to have Presentation Channels");
    }

    @Override
    public Optional<String> resolveMasterBrandId(Content content) {
        throw new UnsupportedOperationException("Unbox cannot resolve serviceIds because its content "
                                                + "is not expected to have Presentation Channels");
    }
}
