package org.atlasapi.feeds.youview;

import java.util.UUID;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;


public class UniqueIdGenerator implements IdGenerator {

    @Override
    public String generateVersionCrid(Item item, Version version) {
        return randomString();
    }

    @Override
    public String generateContentCrid(Content content) {
        return randomString();
    }

    @Override
    public String generateOnDemandImi(Item item, Version version, Encoding encoding,
            Location location) {
        return randomString();
    }

    @Override
    public String generateBroadcastImi(String youViewServiceId, Broadcast broadcast) {
        return randomString();
    }

    private String randomString() {
        return UUID.randomUUID().toString();
    }

}
