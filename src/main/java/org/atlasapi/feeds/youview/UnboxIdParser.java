package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;


public class UnboxIdParser implements IdParser {

    private static final String VERSION_SUFFIX = "_version";
    private static final String UNBOX_URI_PATTERN = "http:\\/\\/unbox\\.amazon\\.co\\.uk\\/[a-z]*\\/";
    
    private static String getId(Content content) {
        return content.getCanonicalUri().replaceAll(UNBOX_URI_PATTERN, "");
    }

    @Override
    public String createCrid(String cridPrefix, Content content) {
        return cridPrefix + getId(content);
    }

    @Override
    public String createVersionCrid(String cridPrefix, Content content) {
        return createCrid(cridPrefix, content) + VERSION_SUFFIX;
    }

    @Override
    public String createImi(String imiPrefix, Item item) {
        return imiPrefix + getId(item);
    }
}
