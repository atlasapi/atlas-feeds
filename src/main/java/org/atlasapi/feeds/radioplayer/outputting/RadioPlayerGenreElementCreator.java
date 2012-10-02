package org.atlasapi.feeds.radioplayer.outputting;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Element;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.entity.Item;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

public class RadioPlayerGenreElementCreator {

    private final RadioPlayerCSVReadingGenreMap GENRES;
    protected static final XMLNamespace EPGDATATYPES = new XMLNamespace("epg", "http://www.radioplayer.co.uk/schemas/11/epgDataTypes");
    private final List<String> genreTypes = ImmutableList.of("main", "secondary", "secondary", "secondary");
    private final Ordering<String> genreTypeComparator = Ordering.explicit(genreTypes.subList(0, 2));

    public RadioPlayerGenreElementCreator() {
        this(new RadioPlayerCSVReadingGenreMap(RadioPlayerCSVReadingGenreMap.GENRES_FILE));
    }

    public RadioPlayerGenreElementCreator(RadioPlayerCSVReadingGenreMap radioPlayerCSVReadingGenreMap) {
        this.GENRES = radioPlayerCSVReadingGenreMap;
    }

    public List<Element> genreElementsFor(Item item) {

        Iterable<List<String>> mappedGenres = GENRES.map(item.getGenres());

        final Map<String, String> finalGenres = Maps.newHashMap();

        // Create map of genre to type backwards, so most significant genre type
        // definition is left in map.
        for (int i = genreTypes.size() - 1; i >= 0; i--) {
            for (List<String> genreGroup : mappedGenres) {
                if (genreGroup.size() > i) {
                    finalGenres.put(genreGroup.get(i), genreTypes.get(i));
                }
            }
        }

        // Order keys by type.
        Iterable<String> orderedKeys = Ordering.from(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return genreTypeComparator.compare(finalGenres.get(o1), finalGenres.get(o2));
            }
        }).sortedCopy(finalGenres.keySet());

        // Create the actual genre elements
        ImmutableList<Element> genreElements = ImmutableList.copyOf(Iterables.transform(orderedKeys, new Function<String, Element>() {
            @Override
            public Element apply(String genre) {
                return createGenreElement(genre, finalGenres.get(genre));
            }
        }));

        return genreElements;
    }

    private Element createGenreElement(String genre, String type) {
        Element elem = new Element("genre", EPGDATATYPES.getUri());
        elem.setNamespacePrefix(EPGDATATYPES.getPrefix());
        elem.addAttribute(new Attribute("type", type));
        elem.addAttribute(new Attribute("href", genre));
        return elem;
    }
}
