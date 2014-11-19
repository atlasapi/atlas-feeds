package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atlasapi.feeds.youview.genres.GenreMapping;
import org.atlasapi.media.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;


public class NitroGenreMapping implements GenreMapping {

    private static final String GENRE_PREFIX = "http://nitro.bbc.co.uk/genres/";
    private static final String TOP_LEVEL_GENRE_PATTERN = GENRE_PREFIX + "1[0-9]*";
    private static final String SECOND_LEVEL_GENRE_PATTERN = GENRE_PREFIX + "2[0-9]*";
    private static final String THIRD_LEVEL_GENRE_PATTERN = GENRE_PREFIX + "3[0-9]*";
    private static final Function<List<String>, BbcGenreTree> TO_GENRE_TREE = 
            new Function<List<String>, BbcGenreTree>() {
        @Override
        public BbcGenreTree apply(List<String> input) {
            Iterator<String> iterator = input.iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            BbcGenreTree.Builder genreTree = BbcGenreTree.builder(iterator.next());
            if (iterator.hasNext()) {
                genreTree.withSecondLevelGenre(iterator.next());
            }
            if (iterator.hasNext()) {
                genreTree.withThirdLevelGenre(iterator.next());
            }
            return genreTree.build();
        }
    };

    private final Function<BbcGenreTree, Set<String>> genreLookup = new Function<BbcGenreTree, Set<String>>() {
        @Override
        public Set<String> apply(BbcGenreTree input) {
            Set<String> yvGenres = youViewGenreMap.get(input);
            if (yvGenres == null) {
                return ImmutableSet.of();
            }
            return yvGenres;
        }
    };
    
    private final Logger log = LoggerFactory.getLogger(NitroServiceMapping.class);
    private final Map<BbcGenreTree, Set<String>> youViewGenreMap;
    
    public NitroGenreMapping(String fileName) {
        this.youViewGenreMap = readFile(checkNotNull(fileName));
    }

    @Override
    public Set<String> youViewGenresFor(Content content) {
        Set<BbcGenreTree> bbcGenres = genresFrom(content);
        return FluentIterable.from(bbcGenres)
                .transformAndConcat(genreLookup)
                .filter(Predicates.notNull())
                .toSet();
    }
    // this is inefficient and clunky TODO improve it
    private Set<BbcGenreTree> genresFrom(Content content) {
        Set<String> topLevelGenres = identifyGenres(TOP_LEVEL_GENRE_PATTERN, content.getGenres());
        Set<String> secondLevelGenres = identifyGenres(SECOND_LEVEL_GENRE_PATTERN, content.getGenres());
        Set<String> thirdLevelGenres = identifyGenres(THIRD_LEVEL_GENRE_PATTERN, content.getGenres());
        
        if (secondLevelGenres.isEmpty()) {
            return FluentIterable.from(Sets.cartesianProduct(topLevelGenres))
                    .transform(TO_GENRE_TREE)
                    .toSet();
        }
        if (thirdLevelGenres.isEmpty()) {
            return FluentIterable.from(Sets.cartesianProduct(topLevelGenres, secondLevelGenres))
                    .transform(TO_GENRE_TREE)
                    .toSet();
        }
        return FluentIterable.from(Sets.<String>cartesianProduct(topLevelGenres, secondLevelGenres, thirdLevelGenres))
            .transform(TO_GENRE_TREE)
            .toSet();
    }
    
    private Set<String> identifyGenres(final String genrePattern, Set<String> genres) {
        return FluentIterable.from(genres)
                .filter(new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        return input.matches(genrePattern);
                    }
                })
                .toSet();
    }

    private Map<BbcGenreTree, Set<String>> readFile(String fileName) {
        try {
            URL resource = Resources.getResource(getClass(), fileName);
            InputSupplier<InputStreamReader> supplier = Resources.newReaderSupplier(resource, Charsets.UTF_8);

            return CharStreams.readLines(supplier, new NitroGenreMappingLineProcessor(GENRE_PREFIX));
        } catch (IOException e) {
            log.error(String.format("Error reading genre file %s", fileName), e);
            return null;
        }    
    }
}
