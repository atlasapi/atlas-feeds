package org.atlasapi.feeds.youview.nitro;

import org.atlasapi.media.entity.Content;

public class NitroUtils {

    private static final String ENGLISH_LANG = "en";
    private static final String GAELIC_LANG = "gla";
    private static final String ALBA_CHANNEL = "http://ref.atlasapi.org/channels/bbcalba";

    public static String getLanguageCodeFor(Content content) {
        if (ALBA_CHANNEL.equals(content.getPresentationChannel())) {
            return GAELIC_LANG;
        }

        return ENGLISH_LANG;
    }

    public static boolean isGaelic(Content content) {
        return GAELIC_LANG.equals(getLanguageCodeFor(content));
    }

}
