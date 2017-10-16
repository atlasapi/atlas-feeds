package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroServiceIdResolver;
import org.atlasapi.feeds.youview.unbox.UnboxIdGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxServiceIdResolver;
import org.atlasapi.media.entity.Publisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({ NitroTVAnytimeModule.class})
public class ServiceIdResolverFactory {

    @Autowired
    private NitroServiceIdResolver nitroServiceIdResolver;
    private UnboxServiceIdResolver unboxServiceIdResolver;
    public ServiceIdResolver create(Publisher publisher){
        if (publisher == Publisher.BBC_NITRO) {
            return nitroServiceIdResolver;
        }
        else if (publisher == Publisher.AMAZON_UNBOX){
            return unboxServiceIdResolver;
        }

        throw new IllegalArgumentException("There is no known ServiceIdResolver for publisher="+publisher);
    }
}
