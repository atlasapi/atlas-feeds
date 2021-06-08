package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.unbox.AmazonIdGenerator;
import org.atlasapi.media.entity.Publisher;

public class IdGeneratorFactory {
    public static IdGenerator create(Publisher publisher){
        if (publisher == Publisher.BBC_NITRO) {
            return new NitroIdGenerator();
        }
        else if (publisher == Publisher.AMAZON_UNBOX){
            return new AmazonIdGenerator();
        }
        else if (publisher == Publisher.AMAZON_V3){
            return new org.atlasapi.feeds.youview.amazon.AmazonIdGenerator();
        }

        throw new IllegalArgumentException("There is no known idGenerator for publisher="+publisher);
    }
}
