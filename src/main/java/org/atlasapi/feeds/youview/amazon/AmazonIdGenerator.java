package org.atlasapi.feeds.youview.amazon;

import com.metabroadcast.applications.client.model.internal.Environment;
import com.metabroadcast.common.properties.Configurer;
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

import java.util.List;
import java.util.regex.Pattern;

public class AmazonIdGenerator implements IdGenerator {

    private static MbstCridGenerator mbstCridGenerator = new MbstCridGenerator(
            Environment.parse(Configurer.getPlatform()),
            "v3.amazon.co.uk" //amazon crid identifier
    );

    private static Pattern versionPattern = null;

    /**
     * @return crid://metabroadcast.com/v3.amazon.co.uk:content:ID
     */
    @Override
    public String generateContentCrid(Content content) {
        return mbstCridGenerator.getContentCrid(content);
    }

    /**
     * @return crid://metabroadcast.com/v3.amazon.co.uk:content:ID/version
     */
    @Override
    public String generateVersionCrid(Item item, Version version) {
        //it was a YV's requirement that we present everything under a single version, so the ID
        //generation assumes this.
        return mbstCridGenerator.getVersionCrid(item);
    }

    /**
     * @return imi:metabroadcast.com/v3.amazon.co.uk:content:ID:ondemand:QUALITY
     */
    @Override
    public String generateOnDemandImi(Item item, Version version, Encoding encoding,
            List<Location> locations) {

        return mbstCridGenerator.getOndemandImi(
                item, MbstCridGenerator.ATLAS_TO_YV_QUALITY_MAPPING.get(encoding.getQuality()));
    }
    
    @Override
    public String generateBroadcastImi(String serviceId, Broadcast broadcast) {
        throw new UnsupportedOperationException("Broadcasts are not supported for the Amazon V3 publisher");
    }

    @Override
    public String generateChannelCrid(Channel channel) {
        throw new UnsupportedOperationException("Channels are not supported for the Amazon V3 publisher");
    }

    /**
     * CannonicalUris for amazon locations look like this
     * http://gb.amazon.com/asin/B01MFA4GML/SUBSCRIPTION
     * (they are set in main atlas, AmazonUnboxContentExtractor).
     */
    public static String getAsin(Location content) {
        String[] splinters = content.getCanonicalUri().split("/");
        return splinters[splinters.length - 2];
    }
}
