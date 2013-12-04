package org.atlasapi.feeds.radioplayer.outputting;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;


public class RadioPlayerIdGenreMap implements RadioPlayerGenreMap {

    public static final String GENRES_FILE = "bbcIdRadioplayerUrnMapping.tsv";
    private ListMultimap<String, String> genreMapping;

    public RadioPlayerIdGenreMap(String genreResourceFilename, String prefix) {
        this(Resources.getResource(genreResourceFilename), prefix);
    }

    public RadioPlayerIdGenreMap(URL genreResourceLocation, String prefix) {
        try {
            this.genreMapping = loadGenres(genreResourceLocation, prefix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    private ListMultimap<String, String> loadGenres(URL genreResourceLocation, final String prefix)
            throws IOException {
        return Resources.readLines(genreResourceLocation, Charsets.UTF_8, 
            new LineProcessor<ListMultimap<String, String>>() {

                private final Splitter onTabs = Splitter.on('\t').omitEmptyStrings().trimResults();
                private final ImmutableListMultimap.Builder<String, String> mapping
                    = ImmutableListMultimap.builder();
            
                @Override
                public boolean processLine(String line) throws IOException {
                    List<String> parts = onTabs.splitToList(line);
                    mapping.put(prefix + parts.get(0), parts.get(1));
                    return true;
                }

                @Override
                public ListMultimap<String, String> getResult() {
                    return mapping.build();
                }
            }
        );
    }

    @Override
    public Set<List<String>> map(Set<String> sourceGenres) {
        Set<List<String>> mappedGenres = Sets.newHashSet();
        
        if (sourceGenres == null) {
            return mappedGenres;
        }

        for (String genre : sourceGenres) {
            mappedGenres.add(ImmutableList.copyOf(genreMapping.get(genre)));
        }

        return mappedGenres;
    }

}
