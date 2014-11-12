package org.atlasapi.feeds.youview.nitro;

import java.util.Set;

import org.atlasapi.feeds.youview.genres.GenreMapping;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;


public class NitroGenreMapping implements GenreMapping {

    @Override
    public Set<String> getYouViewGenresFor(String genre) {
        // TODO Auto-generated method stub
        return ImmutableSet.of();
    }

    @Override
    public Function<String, String> toAtlasGenre() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Function<String, String> toAtlasSubGenre() {
        // TODO Auto-generated method stub
        return null;
    }

}
