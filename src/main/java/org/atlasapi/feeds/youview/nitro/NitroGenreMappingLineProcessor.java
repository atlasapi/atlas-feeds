package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joptsimple.internal.Strings;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.LineProcessor;

public class NitroGenreMappingLineProcessor implements LineProcessor<Map<BbcGenreTree, Set<String>>> {

    private static final Splitter ON_COMMA = Splitter.on(",");
    
    private final String genresPrefix;
    
    public NitroGenreMappingLineProcessor(String genresPrefix) {
        this.genresPrefix = checkNotNull(genresPrefix);
    }
    
    private boolean headersSeen = false;
    ImmutableMap.Builder<BbcGenreTree, Set<String>> mapping = ImmutableMap.builder();

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
        if (Iterables.size(values) < 8) {
            throw new RuntimeException("Error processing line " + line + ", expected 8 items per line");
        }
        
        BbcGenreTree.Builder genreTree = BbcGenreTree.builder(genresPrefix + values.get(0));
        
        if (!Strings.isNullOrEmpty(values.get(1))) {
            genreTree.withSecondLevelGenre(genresPrefix + stripQuotationMarks(values.get(1)));
        }
        if (!Strings.isNullOrEmpty(values.get(2))) {
            genreTree.withThirdLevelGenre(genresPrefix + stripQuotationMarks(values.get(2)));
        }
        
        mapping.put(genreTree.build(), parseYouViewGenres(Iterables.skip(values, 4)));
        return true;
    }

    private ImmutableSet<String> parseYouViewGenres(Iterable<String> genres) {
        return ImmutableSet.copyOf(Iterables.filter(
                Iterables.transform(genres, stripQuotationMarks()), 
                Predicates.notNull()
        ));
    }
    
    private static Function<String, String> stripQuotationMarks() {
        return new Function<String, String>() {
            @Override
            public String apply(String input) {
                if (Strings.isNullOrEmpty(input)) {
                    return null;
                }
                return stripQuotationMarks(input);
            }
        };
    }
    
    private static String stripQuotationMarks(String input) {
        return input.replace("\"", "");
    }

    @Override
    public Map<BbcGenreTree, Set<String>> getResult() {
        return mapping.build();
    }
}