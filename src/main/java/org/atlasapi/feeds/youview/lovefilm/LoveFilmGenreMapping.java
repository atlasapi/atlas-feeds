package org.atlasapi.feeds.youview.lovefilm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import org.atlasapi.feeds.youview.genres.GenreMapping;
import org.atlasapi.feeds.youview.genres.GenreMappingLineProcessor;
import org.atlasapi.feeds.youview.genres.GenreTransformer;
import org.atlasapi.feeds.youview.genres.OldGenreMapping;
import org.atlasapi.media.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;


public class LoveFilmGenreMapping implements GenreMapping, OldGenreMapping {

    private static final String LOVEFILM_GENRE_FILENAME = "LOVEFiLM_YouView_GenreMapping.csv";
    private static final String LOVEFILM_GENRE_PREFIX = "http://lovefilm.com/genres/";
    
    private final Logger log = LoggerFactory.getLogger(LoveFilmGenreMapping.class);
    
    private final Function<String, Iterable<String>> toYouViewGenres = new Function<String, Iterable<String>>() {
        @Override
        public Iterable<String> apply(String input) {
            return getYouViewGenresFor(input);
        }
    };
    
    private final Multimap<String, String> genreMapping;

    public LoveFilmGenreMapping() {
        this.genreMapping = generateLines();
    }

    @Override
    public Set<String> youViewGenresFor(Content content) {
        return FluentIterable.from(content.getGenres())
                .transformAndConcat(toYouViewGenres)
                .toSet();
    }
    
    @Override
    public final Function<String, String> toAtlasGenre() {
        return new Function<String, String>() {
            @Override
            public String apply(String input) {
                return GenreTransformer.toAtlasGenre(LOVEFILM_GENRE_PREFIX, input);
            }
        };
    }
    
    @Override
    public final Function<String, String> toAtlasSubGenre() {
        return new Function<String, String>() {
            @Override
            public String apply(String input) {
                return GenreTransformer.toAtlasSubGenre(LOVEFILM_GENRE_PREFIX, input);
            }
        };
    }
    
    @Override
    public final Collection<String> getYouViewGenresFor(String genre) {
        return genreMapping.get(genre);
    }

    private Multimap<String, String> generateLines() {
        try {
            URL resource = Resources.getResource(getClass(), LOVEFILM_GENRE_FILENAME);
            InputSupplier<InputStreamReader> supplier = Resources.newReaderSupplier(resource, Charsets.UTF_8);

            return CharStreams.readLines(supplier, new GenreMappingLineProcessor(LOVEFILM_GENRE_PREFIX));
        } catch (IOException e) {
            log.error(String.format("Error reading genre file %s", LOVEFILM_GENRE_FILENAME), e);
            return null;
        }
    }
}
