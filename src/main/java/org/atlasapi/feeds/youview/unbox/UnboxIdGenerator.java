package org.atlasapi.feeds.youview.unbox;

import java.util.regex.Pattern;

import org.atlasapi.feeds.MbstCridGenerator;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;

import com.metabroadcast.applications.client.model.internal.Environment;
import com.metabroadcast.common.properties.Configurer;

public class UnboxIdGenerator implements IdGenerator {

    private static final String AMAZON_IMI_PREFIX = "imi:amazon.com/";
    private static final Environment ENVIRONMENT = Environment.parse(Configurer.getPlatform());

    public static final String VERSION_SUFFIX = "_version";
    public static final String AMAZON_CRID_IDENTIFIER = "amazon.com";
    
    @Override
    public String generateVersionCrid(Item item, Version version) {
        //we cannot base the version on the parent id, because we might have multiple versions
        //and they need different crids. Since versions themselves dont have ids, we cannot generate
        //mbst style crids. But it shouldn't matter, because we dont want to use the rep-id service
        //on versions so amazon-like crids should be fine.
        return "crid://amazon.com/exec/obidos/ASIN/" +getAsin(version) + VERSION_SUFFIX;
    }

    @Override
    public String generateContentCrid(Content content) {
        return baseCridFrom(content);
    }
    
    @Override
    // If you ever want to create different IMIs for content that is available via 2 means
    // (e.g. one ASIN for both subscription and pay_to_buy)
    // consider using either the canonical uri, or location.getPolicy().getRevenueContract().
    public String generateOnDemandImi(Item item, Version version, Encoding encoding, Location location) {
        return AMAZON_IMI_PREFIX + getAsin(location);
    }
    
    @Override
    public String generateBroadcastImi(String serviceId, Broadcast broadcast) {
        throw new UnsupportedOperationException("Broadcasts are not supported for the Amazon Unbox publisher");
    }

    @Override
    public String generateChannelCrid(Channel channel) {
        throw new UnsupportedOperationException("Channels are not supported for the Amazon Unbox publisher");
    }

    public static Pattern getVersionCridPattern(){
        return Pattern.compile("crid://amazon.com/exec/obidos/ASIN/" + "[A-Za-z0-9]*" + VERSION_SUFFIX);
    }

    private static String baseCridFrom(Identified content) {
        return MbstCridGenerator.getContentCrid(AMAZON_CRID_IDENTIFIER, ENVIRONMENT, content);
    }

    /**
     * Uris for amazon content look like http://unbox.amazon.co.uk/B00EV9L5LE
     */
    private static String getAsin(Identified content) {
        String[] splinters = content.getCanonicalUri().split("/");
        return splinters[splinters.length - 1];
    }

    /**
     * CannonicalUris for amazon locations look like this
     * http://www.amazon.co.uk/gp/product/B072NZYNMT/PAY_TO_RENT
     * (they are set in main atlas, AmazonUnboxContentExtractor).
     */
    private static String getAsin(Location content) {
        String[] splinters = content.getCanonicalUri().split("/");
        return splinters[splinters.length - 2];
    }
}
