package org.atlasapi.feeds.youview.services;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.io.LineProcessor;

public class ServiceMappingLineProcessor implements LineProcessor<Multimap<String, String>> {

    private static final Splitter ON_COMMA = Splitter.on(",").omitEmptyStrings();
    
    public ServiceMappingLineProcessor() {
    }
    
    private boolean headersSeen = false;
    ImmutableMultimap.Builder<String, String> mapping = ImmutableMultimap.builder();

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
        // line should contain at least service id, service location, youview service id
        // service locator type is often not present, and not required
        if (Iterables.size(values) < 3) {
            // TODO throw?
            return true;
        }
        String bbcSId = values.get(0).toLowerCase();
        String youViewSId = values.get(2).toLowerCase();
        mapping.put(bbcSId, youViewSId);
        
        return true;
    }

    @Override
    public Multimap<String, String> getResult() {
        return mapping.build();
    }
}