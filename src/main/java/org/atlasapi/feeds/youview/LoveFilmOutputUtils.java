package org.atlasapi.feeds.youview;

public class LoveFilmOutputUtils {

    private static final String LOVEFILM_URI_PATTERN = "http:\\/\\/lovefilm\\.com\\/[a-z]*\\/";
    
    public static String getId(String uri) {
        return uri.replaceAll(LOVEFILM_URI_PATTERN, "");
    }
}
