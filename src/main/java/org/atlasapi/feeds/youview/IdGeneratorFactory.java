package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.feeds.youview.unbox.UnboxIdGenerator;
import org.atlasapi.media.entity.Publisher;

public class IdGeneratorFactory {
    public static IdGenerator create(Publisher publisher){
        if (publisher == Publisher.BBC_NITRO) {
            return new NitroIdGenerator();
        }
        else if (publisher == Publisher.AMAZON_UNBOX){
            return new UnboxIdGenerator();
        }

        throw new IllegalArgumentException("There is no known idGenerator for publisher="+publisher);
    }
}
