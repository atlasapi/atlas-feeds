package org.atlasapi.feeds.radioplayer.outputting;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class RadioPlayerIdGenreMapTest {

    private final String NO_PREFIX = "";
    private final RadioPlayerIdGenreMap gm = new RadioPlayerIdGenreMap(
            Resources.getResource(getClass(), RadioPlayerIdGenreMap.GENRES_FILE),
            NO_PREFIX
            );

    @Test
    @SuppressWarnings("unchecked")
    public void testMap() {

        testMapping(ImmutableSet.of("200004"), ImmutableList.of(
                "urn:radioplayer:metadata:cs:Category:2012:6",
                "urn:radioplayer:metadata:cs:Category:2012:7"));
        testMapping(ImmutableSet.of("200004", "200006"), ImmutableList.of(
                "urn:radioplayer:metadata:cs:Category:2012:6",
                "urn:radioplayer:metadata:cs:Category:2012:7"));
    }

    private void testMapping(ImmutableSet<String> genres, List<String>...expected) {
        Set<List<String>> mapped = gm.map(genres);
        assertEquals(ImmutableSet.copyOf(expected), mapped);
    }

}
