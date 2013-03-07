package org.atlasapi.feeds.youview;

import static org.atlasapi.feeds.utils.lovefilm.LoveFilmGenreConverter.TO_ATLAS_GENRE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
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

public class YouViewGenreMapping {
    
    private final Logger log = LoggerFactory.getLogger(YouViewGenreMapping.class);
    
    private final Multimap<String, String> mapping;

    public YouViewGenreMapping(String filePath) {
        this.mapping = generateLines(filePath);
    }

    public Collection<String> get(String key) {
        return mapping.get(key);
    }

    private Multimap<String, String> generateLines(String fileName) {
        try {
            URL resource = Resources.getResource(getClass(), fileName);
            InputSupplier<InputStreamReader> supplier = Resources.newReaderSupplier(resource, Charsets.UTF_8);
//            final File genreFile = new File(filePath);
//
//            InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
//                @Override
//                public InputStream getInput() throws FileNotFoundException {
//                    return new FileInputStream(genreFile);
//                }
//            };
//
//            InputSupplier<InputStreamReader> supplier = CharStreams.newReaderSupplier(inputSupplier, Charsets.UTF_8);

            return CharStreams.readLines(supplier, new GenreMappingLineProcessor());
        } catch (IOException e) {
            log.error(String.format("Error reading genre file %s", fileName), e);
            return null;
        }
    }
    
    private static class GenreMappingLineProcessor implements LineProcessor<Multimap<String, String>> {

        private static final Splitter ON_COMMA = Splitter.on(",");
        
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
            String key = TO_ATLAS_GENRE.apply(values.get(0));
            for (String value : Iterables.skip(values, 1)) { 
                mapping.put(key, value);
            }
            
            return false;
        }

        @Override
        public Multimap<String, String> getResult() {
            return mapping.build();
        }
    }
}
