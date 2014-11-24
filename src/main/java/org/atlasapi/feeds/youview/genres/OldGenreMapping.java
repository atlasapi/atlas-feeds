package org.atlasapi.feeds.youview.genres;

import java.util.Collection;

import com.google.common.base.Function;


// This is the original genre mapping interface as used for the LOVEFiLM and Amazon
// Unbox TVA output. However, the publisher-specific implementations are tied up with their
// respective ingesters, so are hard to rewire to be more general.
// TODO pull out the elements used in LF/Unbox ingesters to a separate interface
public interface OldGenreMapping {
    
    Collection<String> getYouViewGenresFor(String genre);
    Function<String, String> toAtlasGenre();
    Function<String, String> toAtlasSubGenre();
}
