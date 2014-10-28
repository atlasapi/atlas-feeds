package org.atlasapi.feeds.youview.genres;

import java.util.Collection;

import com.google.common.base.Function;


public interface GenreMapping {
    
    Collection<String> getYouViewGenresFor(String genre);
    Function<String, String> toAtlasGenre();
    Function<String, String> toAtlasSubGenre();
}
