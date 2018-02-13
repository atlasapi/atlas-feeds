package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Content;


public class YouViewGeneratorUtils {

    public static final String ASIN_NAMESPACE = "gb:amazon:asin";
    
    public static String getAsin(Content content) {
        for (Alias alias : content.getAliases()) {
            if (alias.getNamespace().equals(ASIN_NAMESPACE)) {
                return alias.getValue();
            }
        }
        throw new RuntimeException("no ASIN on " + content.getCanonicalUri());
    }
    
    public static boolean hasAsin(Content content) {
        for (Alias alias : content.getAliases()) {
            if (alias.getNamespace().equals(ASIN_NAMESPACE)) {
                return true;
            }
        }
        return false;
    }
}
