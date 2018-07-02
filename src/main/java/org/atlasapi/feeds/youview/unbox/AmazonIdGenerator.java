package org.atlasapi.feeds.youview.unbox;

import java.util.List;
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

public class AmazonIdGenerator implements IdGenerator {

    private static MbstCridGenerator mbstCridGenerator = new MbstCridGenerator(
            Environment.parse(Configurer.getPlatform()),
            "amazon.com" //amazon crid identifier
    );

    private static Pattern versionPattern = null;

    /**
     * @return crid://amazon.com/metabroadcast.com/ITEM_ASIN
     */
    @Override
    public String generateContentCrid(Content content) {
        return mbstCridGenerator.getContentCrid(content);
    }

    /**
     * @return crid://amazon.com/metabroadcast.com/ITEM_ASIN/version
     */
    @Override
    public String generateVersionCrid(Item item, Version version) {
        //This assumes that each item only has a single version on it. That is not the case in atlas
        //where each content is stored with its own version (that is based on the content's if), but
        //it was a YV's requirement that we present everything under a single version, so the ID
        //generation assumes this.
        return mbstCridGenerator.getVersionCrid(item);
    }

    /**
     * @return crid://amazon.com/metabroadcast.com/ITEM_ASIN/ondemand/QUALITY
     */
    @Override
    public String generateOnDemandImi(Item item, Version version, Encoding encoding,
            List<Location> locations) {

        return mbstCridGenerator.getOndemandImi(
                item, MbstCridGenerator.ATLAS_TO_YV_QUALITY_MAPPING.get(encoding.getQuality()));

        /* This is what should be happening. But amazon does not give us actual resolutions,
        so we just pass on whatever we got from amazon to YV.

        //decide on content quality based on YV's standard (SPECWIP-4212)
        MbstCridGenerator.Quality quality = MbstCridGenerator.Quality.SD;
        Integer size = encoding.getVideoHorizontalSize();
        if (size == null) {
            size = 0; //assume SD
        }
        if (size < 720) {
            quality = MbstCridGenerator.Quality.SD;
        } else if (720 <= size && size < 2160) {
            quality = MbstCridGenerator.Quality.HD;
        } else if (2160 <= size) {
            quality = MbstCridGenerator.Quality.UHD;
        }
        return mbstCridGenerator.getOndemandImi(item, quality);
        */
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
        if(versionPattern == null) {
            //version format is crid://metabroadcast.com/amazon.com/ITEM_ASIN/version
            Identified identified = new Identified();
            identified.setId(51955835295115080L); //will encode to nnnnnnnnnnnn
            String versionCrid = mbstCridGenerator.getVersionCrid(identified);
            String firstPart = versionCrid.substring(0, versionCrid.indexOf("nnnnnnnnnnnn"));
            String lastPart = versionCrid.substring(
                    versionCrid.indexOf("nnnnnnnnnnnn") + 12,
                    versionCrid.length());
            versionPattern = Pattern.compile(firstPart + "[A-Za-z0-9]*" + lastPart);
        }
        return versionPattern;
    }

    /**
     * Uris for amazon content look like http://unbox.amazon.co.uk/B00EV9L5LE
     */
    public static String getAsin(Identified content) {
        String[] splinters = content.getCanonicalUri().split("/");
        return splinters[splinters.length - 1];
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
