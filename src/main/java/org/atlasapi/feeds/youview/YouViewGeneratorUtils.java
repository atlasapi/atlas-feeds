package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class YouViewGeneratorUtils {

    public static final String ASIN_NAMESPACE_UNBOX = "gb:amazon:asin";
    private static final Pattern ASIN_NAMESPACE_AMAZON_PATTERN = Pattern.compile("amazon:asin:\\d+");

    public static String getAmazonAsin(Content content) {
        for (Alias alias : content.getAliases()) {
            Matcher matcher = ASIN_NAMESPACE_AMAZON_PATTERN.matcher(alias.getNamespace());
            if (matcher.matches()) {
                return alias.getValue();
            }
        }
        throw new RuntimeException("no ASIN on " + content.getCanonicalUri());
    }

    public static boolean hasAmazonAsin(Content content) {
        for (Alias alias : content.getAliases()) {
            Matcher matcher = ASIN_NAMESPACE_AMAZON_PATTERN.matcher(alias.getNamespace());
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }
    
    public static String getUnboxAsin(Content content) {
        for (Alias alias : content.getAliases()) {
            if (alias.getNamespace().equals(ASIN_NAMESPACE_UNBOX)) {
                return alias.getValue();
            }
        }
        throw new RuntimeException("no ASIN on " + content.getCanonicalUri());
    }
    
    public static boolean hasUnboxAsin(Content content) {
        for (Alias alias : content.getAliases()) {
            if (alias.getNamespace().equals(ASIN_NAMESPACE_UNBOX)) {
                return true;
            }
        }
        return false;
    }
}
