package org.atlasapi.feeds.youview.unbox;

import org.atlasapi.feeds.youview.services.BroadcastServiceMapping;


public class AmazonBroadcastServiceMapping implements BroadcastServiceMapping {

    @Override
    public Iterable<String> youviewServiceIdFor(String bbcServiceId) {
        throw new UnsupportedOperationException("Amazon Unbox catalog does not contain broadcast data");
    }

}
