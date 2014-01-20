package org.atlasapi.feeds.youview;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;


public class UnboxGenreMapping implements GenreMapping {

    private static final String UNBOX_GENRE_FILENAME = "Amazon_Unbox_YouView_GenreMapping.csv";
    
    private final Logger log = LoggerFactory.getLogger(UnboxGenreMapping.class);
    
    private final Multimap<String, String> genreMapping;
    
    public UnboxGenreMapping() {
        this.genreMapping = generateLines();
    }
    
    @Override
    public Collection<String> getYouViewGenresFor(String genre) {
        return genreMapping.get(genre);
    }

    private Multimap<String, String> generateLines() {
        try {
            URL resource = Resources.getResource(getClass(), UNBOX_GENRE_FILENAME);
            InputSupplier<InputStreamReader> supplier = Resources.newReaderSupplier(resource, Charsets.UTF_8);

            return CharStreams.readLines(supplier, new GenreMappingLineProcessor());
        } catch (IOException e) {
            log.error(String.format("Error reading genre file %s", UNBOX_GENRE_FILENAME), e);
            return null;
        }
    }
    
    private static class GenreMappingLineProcessor implements LineProcessor<Multimap<String, String>> {

        private static final Splitter ON_COMMA = Splitter.on(",").omitEmptyStrings();
        
        private boolean headersSeen = false;
        Builder<String, String> mapping = ImmutableMultimap.builder();

        @Override
        public boolean processLine(String line) throws IOException {
            if (!headersSeen) {
                headersSeen = true;
                return true;
            }
            return processRow(line);
        }

        private boolean processRow(String line) {
            List<String> values = ImmutableList.copyOf(ON_COMMA.split(line));
            if (Iterables.size(values) < 2) {
                return true;
            }
            String genre = values.get(0);
            String key = null;
            if (IS_SUB_GENRE.apply(genre)) {
                key = TO_ATLAS_SUB_GENRE.apply(genre);                
            } else {
                key = TO_ATLAS_GENRE.apply(genre);
            }
            for (String value : Iterables.skip(values, 1)) { 
                mapping.put(key, value);
            }
            
            return true;
        }

        @Override
        public Multimap<String, String> getResult() {
            return mapping.build();
        }
        
        private static final String UNBOX_GENRES_PREFIX = "http://unbox.amazon.co.uk/genres/";
        
        private static final Predicate<String> IS_SUB_GENRE = new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.contains(" > ");
            }
        };
        
        private static final Function<String, String> TO_ATLAS_GENRE = new Function<String, String>() {
            @Override
            public String apply(@Nullable String input) {
                input = input.toLowerCase();
                return UNBOX_GENRES_PREFIX + input.replace('/', '-').replace(" ", "");
            }
        };
        
        private static final Function<String, String> TO_ATLAS_SUB_GENRE = new Function<String, String>() {
            @Override
            public String apply(@Nullable String input) {
                input = input.toLowerCase();
                return UNBOX_GENRES_PREFIX + input.replace('/', '-').replace(" ", "").replace(">", "/");
            }
        };
    }
}
