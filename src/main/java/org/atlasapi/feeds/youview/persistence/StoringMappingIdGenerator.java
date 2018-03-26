package org.atlasapi.feeds.youview.persistence;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;

public class StoringMappingIdGenerator implements IdGenerator {

    private final IdMappingStore idMappingStore;
    private final IdGenerator delegate;

    public StoringMappingIdGenerator(IdMappingStore idMappingStore, IdGenerator delegate) {
        this.idMappingStore = checkNotNull(idMappingStore);
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public String generateVersionCrid(Item item, Version version) {
        String versionCrid = delegate.generateVersionCrid(item, version);
        idMappingStore.storeMapping(version.getCanonicalUri(), versionCrid);

        return versionCrid;
    }

    @Override
    public String generateContentCrid(Content content) {
        return delegate.generateContentCrid(content);
    }

    @Override
    public String generateOnDemandImi(Item item, Version version, Encoding encoding,
            List<Location> locations) {
        return delegate.generateOnDemandImi(item, version, encoding, locations);
    }

    @Override
    public String generateBroadcastImi(String youViewServiceId, Broadcast broadcast) {
        return delegate.generateBroadcastImi(youViewServiceId, broadcast);
    }

    @Override
    public String generateChannelCrid(Channel channel) {
        return delegate.generateChannelCrid(channel);
    }
}
