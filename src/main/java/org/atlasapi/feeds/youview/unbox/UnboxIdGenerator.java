package org.atlasapi.feeds.youview.unbox;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;


public class UnboxIdGenerator implements IdGenerator {

    private static final String UNBOX_IMI_PREFIX = "imi:amazon.com/";
    private static final String UNBOX_PRODUCT_CRID_PREFIX = "crid://amazon.com/product/";
    private static final String VERSION_SUFFIX = "_version";
    
    @Override
    public String generateVersionCrid(Item item, Version version) {
        return baseCridFrom(item) + VERSION_SUFFIX;
    }

    @Override
    public String generateContentCrid(Content content) {
        return baseCridFrom(content);
    }
    
    @Override
    public String generateOnDemandImi(Item item, Version version, Encoding encoding, Location location) {
        return UNBOX_IMI_PREFIX + idFrom(item);
    }
    
    @Override
    public String generateBroadcastImi(String serviceId, Broadcast broadcast) {
        throw new UnsupportedOperationException("Broadcasts are not supported for the Amazon Unbox publisher");
    }

    @Override
    public String generateChannelCrid(Channel channel) {
        throw new UnsupportedOperationException("Channels are not supported for the Amazon Unbox publisher");
    }

    private static String baseCridFrom(Content content) {
        return UNBOX_PRODUCT_CRID_PREFIX + idFrom(content);
    }

    private static String idFrom(Content content) {
        String[] splinters = content.getCanonicalUri().split("/");
        return splinters[splinters.length-1];
    }
}
