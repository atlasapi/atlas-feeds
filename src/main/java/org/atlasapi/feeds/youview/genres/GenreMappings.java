package org.atlasapi.feeds.youview.genres;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.media.entity.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;


public abstract class GenreMappings implements GenreMapping {

    private final Logger log = LoggerFactory.getLogger(NitroGenreMapping.class);
    
    private final Multimap<String, String> genreMapping;
    
    // TODO this is a little hokey
    public static GenreMapping mappingFor(Publisher publisher) {
        if (Publisher.LOVEFILM.equals(publisher)) {
            return new LoveFilmGenreMapping();
        }
        if (Publisher.AMAZON_UNBOX.equals(publisher)) {
            return new UnboxGenreMapping();
        }
        if (Publisher.BBC_NITRO.equals(publisher)) {
            return new NitroGenreMapping();
        }
        throw new InvalidPublisherException(publisher);
    }
    
    public GenreMappings() {
        this.genreMapping = generateLines();
    }
    
    @Override
    public final Function<String, String> toAtlasGenre() {
        return new Function<String, String>() {
            @Override
            public String apply(String input) {
                return GenreTransformer.toAtlasGenre(genrePrefix(), input);
            }
        };
    }
    
    @Override
    public final Function<String, String> toAtlasSubGenre() {
        return new Function<String, String>() {
            @Override
            public String apply(String input) {
                return GenreTransformer.toAtlasSubGenre(genrePrefix(), input);
            }
        };
    }
    
    public abstract String genreFileName();
    
    public abstract String genrePrefix();
    
    @Override
    public final Collection<String> getYouViewGenresFor(String genre) {
        return genreMapping.get(genre);
    }

    private Multimap<String, String> generateLines() {
        try {
            URL resource = Resources.getResource(getClass(), genreFileName());
            InputSupplier<InputStreamReader> supplier = Resources.newReaderSupplier(resource, Charsets.UTF_8);

            return CharStreams.readLines(supplier, new GenreMappingLineProcessor(genrePrefix()));
        } catch (IOException e) {
            log.error(String.format("Error reading genre file %s", genreFileName()), e);
            return null;
        }
    }
    
    public static final class LoveFilmGenreMapping extends GenreMappings {

        private static final String LOVEFILM_GENRE_FILENAME = "LOVEFiLM_YouView_GenreMapping.csv";
        private static final String LOVEFILM_GENRE_PREFIX = "http://lovefilm.com/genres/";
        
        @Override
        public String genreFileName() {
            return LOVEFILM_GENRE_FILENAME;
        }

        @Override
        public String genrePrefix() {
            return LOVEFILM_GENRE_PREFIX;
        }
    }
    
    public static final class UnboxGenreMapping extends GenreMappings {

        private static final String UNBOX_GENRE_FILENAME = "Amazon_Unbox_YouView_GenreMapping.csv";
        private static final String UNBOX_GENRE_PREFIX = "http://unbox.amazon.co.uk/genres/";
        
        @Override
        public String genreFileName() {
            return UNBOX_GENRE_FILENAME;
        }

        @Override
        public String genrePrefix() {
            return UNBOX_GENRE_PREFIX;
        }
    }
    
    public static final class NitroGenreMapping extends GenreMappings {

        private static final String NITRO_GENRE_FILENAME = "Bbc_Nitro_YouView_GenreMapping.csv";
        private static final String NITRO_GENRE_PREFIX = "http://nitro.bbc.co.uk/genres/";
        
        @Override
        public String genreFileName() {
            return NITRO_GENRE_FILENAME;
        }
        
        @Override
        public String genrePrefix() {
            return NITRO_GENRE_PREFIX;
        }
    }
}
