package org.atlasapi.feeds.youview.genres;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.io.LineProcessor;

public class GenreMappingLineProcessor implements LineProcessor<Multimap<String, String>> {

    private static final Splitter ON_COMMA = Splitter.on(",").omitEmptyStrings();
    
    private static final Predicate<String> IS_SUB_GENRE = new Predicate<String>() {
        @Override
        public boolean apply(String input) {
            return input.contains(" > ");
        }
    };
    
    private final String genresPrefix;
    
    public GenreMappingLineProcessor(String genresPrefix) {
        this.genresPrefix = checkNotNull(genresPrefix);
    }
    
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
            key = GenreTransformer.toAtlasSubGenre(genresPrefix, genre);                
        } else {
            key = GenreTransformer.toAtlasGenre(genresPrefix, genre);
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
}