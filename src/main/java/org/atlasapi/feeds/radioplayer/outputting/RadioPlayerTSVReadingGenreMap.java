package org.atlasapi.feeds.radioplayer.outputting;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

public class RadioPlayerTSVReadingGenreMap {

    public static final String GENRES_FILE = "radioplayergenres.tsv";
    private static final String TSV = "([^\\t]+)\\t?";
    private static final Pattern TSV_PATTERN = Pattern.compile(TSV);
    
    private final Log log = LogFactory.getLog(RadioPlayerTSVReadingGenreMap.class);
    private final Multimap<String, String> genreMapping;
//    private final Map<String, List<String>> genreMapping;

    public RadioPlayerTSVReadingGenreMap(String genreResourceFilename) {
        this(Resources.getResource(genreResourceFilename));
    }

    public RadioPlayerTSVReadingGenreMap(URL genreResourceLocation) {
        genreMapping = loadGenres(genreResourceLocation);
    }
    
    private Multimap<String, String> loadGenres(URL url) {
        try {
            return Resources.readLines(url, Charsets.UTF_8, new LineProcessor<Multimap<String, String>>() {
                ImmutableMultimap.Builder<String, String> map = ImmutableMultimap.builder();

                @Override
                public Multimap<String, String> getResult() {
                    return map.build();
                }

                @Override
                public boolean processLine(String line) throws IOException {
                    if (!Strings.isNullOrEmpty(line)) {
                        try {
                            Matcher m = TSV_PATTERN.matcher(line);
                            processMatch(m);
                        } catch (Exception e) {
                            log.warn(line, e);
                        }
                    }
                    return true;
                }
                
                private void processMatch(Matcher m) {
                    String genreUrl;
                    // obtain genre
                    if (m.find()) {
                        genreUrl = m.group().trim();
                    } else {
                        // no genre found on this line
                        return;
                    }
                    while (m.find()) {
                        map.put(genreUrl, m.group().trim());
                    }
                }
            });
        } catch (IOException e) {
            log.warn("Couldn't load genre map", e);
            return ImmutableMultimap.of();
        }
    }

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

    public Multimap<String, String> getMapping() {
        return genreMapping;
    }
}
