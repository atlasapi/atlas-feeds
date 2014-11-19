package org.atlasapi.feeds.youview.lovefilm;

import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;


public class LoveFilmIdGenerator implements IdGenerator {
    
    private static final String LOVEFILM_IMI_PREFIX = "imi:lovefilm.com/";
    private static final String LOVEFILM_PRODUCT_CRID_PREFIX = "crid://lovefilm.com/product/";
    private static final String LOVEFILM_URI_PATTERN = "http:\\/\\/lovefilm\\.com\\/[a-z]*\\/";
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
        return LOVEFILM_IMI_PREFIX + idFrom(item);
    }
    
    @Override
    public String generateBroadcastImi(Broadcast broadcast) {
        throw new UnsupportedOperationException("Broadcasts are not supported for the LOVEFiLM publisher");
    }

    private static String baseCridFrom(Content content) {
        return LOVEFILM_PRODUCT_CRID_PREFIX + idFrom(content);
    }

    private static String idFrom(Content content) {
        return content.getCanonicalUri().replaceAll(LOVEFILM_URI_PATTERN, "");
    }
}
