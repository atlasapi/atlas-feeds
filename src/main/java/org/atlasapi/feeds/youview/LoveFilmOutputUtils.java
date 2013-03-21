package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Content;

public class LoveFilmOutputUtils {

    private static final String LOVEFILM_URI_PATTERN = "http:\\/\\/lovefilm\\.com\\/[a-z]*\\/";
    
    public static String getId(String uri) {
        return uri.replaceAll(LOVEFILM_URI_PATTERN, "");
    }
}
