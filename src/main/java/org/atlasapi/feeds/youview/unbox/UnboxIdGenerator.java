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

public class UnboxIdGenerator implements IdGenerator {

    private static final String AMAZON_IMI_PREFIX = "imi:amazon.com/";
    public static final String VERSION_SUFFIX = "_version";
    
    @Override
    public String generateVersionCrid(Item item, Version version) {
        //we cannot base the version on the parent id, because we might have multiple versions
        //and they need different crids. Since versions themselves dont have ids, we cannot generate
        //mbst style crids. But it shouldn't matter, because we dont want to use the rep-id service
        //on versions so amazon line crids should be fine.
        return "crid://amazon.com/exec/obidos/ASIN/" +getAsin(version) + VERSION_SUFFIX;
    }

    @Override
    public String generateContentCrid(Content content) {
        return baseCridFrom(content);
    }
    
    @Override
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

        return MbstCridGenerator.getContentCrid("stage",content); //todo:hardcoded enviroment.
        //return "crid://amazon.com/exec/obidos/ASIN/" + getAsin(content); old way of generating crids
    }

    private static String getAsin(Identified content) {
        String[] splinters = content.getCanonicalUri().split("/");
        return splinters[splinters.length-1];
    }

    /**
     * CannonicalUris for amazon locations look like this
     *
     * (they are set in main atlas, AmazonUnboxContentExtractor).
     */
    private static String getAsin(Location content) {
        String[] splinters = content.getCanonicalUri().split("/");
        return splinters[splinters.length-2];
    }
}
