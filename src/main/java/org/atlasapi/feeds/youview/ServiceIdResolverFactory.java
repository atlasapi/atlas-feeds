package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.nitro.NitroServiceIdResolver;
import org.atlasapi.feeds.youview.unbox.AmazonServiceIdResolver;
import org.atlasapi.media.entity.Publisher;

import org.springframework.beans.factory.annotation.Autowired;

public class ServiceIdResolverFactory {

    @Autowired
    private NitroServiceIdResolver nitroServiceIdResolver;
    private AmazonServiceIdResolver amazonServiceIdResolver;
    public ServiceIdResolver create(Publisher publisher){
        if (publisher == Publisher.BBC_NITRO) {
            return nitroServiceIdResolver;
        }
        else if (publisher == Publisher.AMAZON_UNBOX){
            return amazonServiceIdResolver;
        }

        throw new IllegalArgumentException("There is no known ServiceIdResolver for publisher="+publisher);
    }
}
