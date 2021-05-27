package org.atlasapi.feeds.youview.amazon;

import com.google.common.base.Optional;
import org.atlasapi.feeds.youview.ServiceIdResolver;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;

public final class AmazonServiceIdResolver implements ServiceIdResolver {

    public AmazonServiceIdResolver() {
    }

    @Override
    public Optional<String> resolveSId(Broadcast broadcast) {
        throw new UnsupportedOperationException(
                "AmazonServiceIdResolver cannot resolve serviceIds, because its "
                + "content is not expected to be broadcasted.");
    }

    @Override
    public Optional<String> resolveSId(Content content) {
        throw new UnsupportedOperationException(
                "AmazonServiceIdResolver cannot resolve serviceIds because its content "
                + "is not expected to have Presentation Channels");
    }

    @Override
    public Optional<String> resolveMasterBrandId(Content content) {
        throw new UnsupportedOperationException(
                "AmazonServiceIdResolver cannot resolve serviceIds because its content "
                + "is not expected to have Presentation Channels");
    }
}
