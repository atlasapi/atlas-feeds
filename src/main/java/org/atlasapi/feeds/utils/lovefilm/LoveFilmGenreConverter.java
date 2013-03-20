package org.atlasapi.feeds.utils.lovefilm;

import javax.annotation.Nullable;

import com.google.common.base.Function;

public class LoveFilmGenreConverter {
    private static final String LOVEFILM_GENRES_PREFIX = "http://lovefilm.com/genres/";
    
    public static final Function<String, String> TO_ATLAS_GENRE = new Function<String, String>() {
        @Override
        public String apply(@Nullable String input) {
            input = input.toLowerCase();
            return LOVEFILM_GENRES_PREFIX + input.replace('/', '-').replace(" ", "");
        }
    };
}
