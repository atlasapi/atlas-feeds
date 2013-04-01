package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.content.Content;

public class LoveFilmOutputUtils {

    private static final String ASIN_NAMESPACE = "gb:amazon:asin";
    private static final String LOVEFILM_URI_PATTERN = "http:\\/\\/lovefilm\\.com\\/[a-z]*\\/";
    
    public static String getId(String uri) {
        return uri.replaceAll(LOVEFILM_URI_PATTERN, "");
    }
    
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
