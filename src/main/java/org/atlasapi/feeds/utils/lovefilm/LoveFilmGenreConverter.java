package org.atlasapi.feeds.utils.lovefilm;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public class LoveFilmGenreConverter {
    private static final String LOVEFILM_GENRES_PREFIX = "http://lovefilm.com/genres/";
    
    public static final Predicate<String> IS_SUB_GENRE = new Predicate<String>() {
        @Override
        public boolean apply(String input) {
            return input.contains(" > ");
        }
    };
    
    public static final Function<String, String> TO_ATLAS_GENRE = new Function<String, String>() {
        @Override
        public String apply(@Nullable String input) {
            input = input.toLowerCase();
            return LOVEFILM_GENRES_PREFIX + input.replace('/', '-').replace(" ", "");
        }
    };
    
    public static final Function<String, String> TO_ATLAS_SUB_GENRE = new Function<String, String>() {
        @Override
        public String apply(@Nullable String input) {
            input = input.toLowerCase();
            return LOVEFILM_GENRES_PREFIX + input.replace('/', '-').replace(" ", "").replace(">", "/");
        }
    };
}
