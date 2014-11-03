package org.atlasapi.feeds.youview.ids;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;


public abstract class IdParsers implements IdParser {

    private static final String VERSION_SUFFIX = "_version";
    
    // TODO this is a little hokey
    public static IdParser parserFor(Publisher publisher) {
        if (Publisher.LOVEFILM.equals(publisher)) {
            return new LoveFilmIdParser();
        }
        if (Publisher.AMAZON_UNBOX.equals(publisher)) {
            return new UnboxIdParser();
        }
        if (Publisher.BBC_NITRO.equals(publisher)) {
            return new NitroIdParser();
        }
        throw new InvalidPublisherException(publisher);
    }
    
    public abstract String uriPattern();
    
    private String getId(Content content) {
        return content.getCanonicalUri().replaceAll(uriPattern(), "");
    }
    
    @Override
    public final String createCrid(String cridPrefix, Content content) {
        return cridPrefix + getId(content);
    }

    @Override
    public final String createVersionCrid(String cridPrefix, Content content) {
        return createCrid(cridPrefix, content) + VERSION_SUFFIX;
    }

    @Override
    public final String createImi(String imiPrefix, Item item) {
        return imiPrefix + getId(item);
    }
    
    public static final class LoveFilmIdParser extends IdParsers {

        private static final String LOVEFILM_URI_PATTERN = "http:\\/\\/lovefilm\\.com\\/[a-z]*\\/";
        
        @Override
        public String uriPattern() {
            return LOVEFILM_URI_PATTERN;
        }
    }
    
    public static final class UnboxIdParser extends IdParsers {

        private static final String UNBOX_URI_PATTERN = "http:\\/\\/unbox\\.amazon\\.co\\.uk\\/[a-z]*\\/";
        
        @Override
        public String uriPattern() {
            return UNBOX_URI_PATTERN;
        }
    }
    
    public static final class NitroIdParser extends IdParsers {

        private static final String NITRO_URI_PATTERN = "http:\\/\\/nitro\\.bbc\\.co\\.uk\\/[a-z]*\\/";
        
        @Override
        public String uriPattern() {
            return NITRO_URI_PATTERN;
        }
    }
}
