package org.atlasapi.feeds.lakeview;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;


public class SortTitleGenerator {

    private final ImmutableMap<String, String> prefixToPrefixWithLeadingSpace;
    
    public SortTitleGenerator(Iterable<String> prefixes) {
        this.prefixToPrefixWithLeadingSpace = Maps.toMap(prefixes, new Function<String, String>() {

            @Override
            public String apply(String prefix) {
                return prefix + " ";
            }
        });
    }
    
    public String createSortTitle(String title) {
        if (title == null) {
            return null;
        }
        for (Map.Entry<String, String> prefix : prefixToPrefixWithLeadingSpace.entrySet()) {
            String prefixWithLeadingSpace = prefix.getValue();
            if (title.startsWith(prefixWithLeadingSpace)) {
                return title.substring(prefixWithLeadingSpace.length()) + ", " + prefix.getKey();
            }
        }
        return title;
    }
    
    
    
}
