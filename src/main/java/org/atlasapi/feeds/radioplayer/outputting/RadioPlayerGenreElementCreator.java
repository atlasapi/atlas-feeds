package org.atlasapi.feeds.radioplayer.outputting;

import java.util.List;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Element;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.entity.Item;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class RadioPlayerGenreElementCreator {

    private final RadioPlayerTSVReadingGenreMap GENRES;
    protected static final XMLNamespace EPGDATATYPES = new XMLNamespace("epg", "http://www.radioplayer.co.uk/schemas/11/epgDataTypes");
    private final List<String> genreTypes = ImmutableList.of("main", "secondary", "secondary", "secondary");
    private final Ordering<String> genreTypeComparator = Ordering.explicit(genreTypes.subList(0, 2));

    public RadioPlayerGenreElementCreator() {
        this(new RadioPlayerTSVReadingGenreMap(RadioPlayerTSVReadingGenreMap.GENRES_FILE));
    }

    public RadioPlayerGenreElementCreator(RadioPlayerTSVReadingGenreMap radioPlayerTSVReadingGenreMap) {
        this.GENRES = radioPlayerTSVReadingGenreMap;
    }

    public List<Element> genreElementsFor(Item item) {

        Iterable<List<String>> mappedGenres = GENRES.map(item.getGenres());
        
        Set<String> genres = Sets.newHashSet();
        for (List<String> genreList : mappedGenres) {
            genres = Sets.union(genres, Sets.newCopyOnWriteArraySet(genreList));
        }

//        final Map<String, String> finalGenres = Maps.newHashMap();

        // THIS TRIMS OFF GENRES...
        // Create map of genre to type backwards, so most significant genre type
        // definition is left in map.
//        for (int i = genreTypes.size() - 1; i >= 0; i--) {
//            for (List<String> genreGroup : mappedGenres) {
//                if (genreGroup.size() > i) {
//                    finalGenres.put(genreGroup.get(i), genreTypes.get(i));
//                }
//            }
//        }

//        // Order keys by type.
//        Iterable<String> orderedKeys = Ordering.from(new Comparator<String>() {
//            @Override
//            public int compare(String o1, String o2) {
//                return genreTypeComparator.compare(finalGenres.get(o1), finalGenres.get(o2));
//            }
//        }).sortedCopy(finalGenres.keySet());

        // Create the actual genre elements
        ImmutableList<Element> genreElements = ImmutableList.copyOf(Iterables.transform(genres, new Function<String, Element>() {
            @Override
            public Element apply(String genre) {
                return createGenreElement(genre);
            }
        }));

        return genreElements;
    }

    private Element createGenreElement(String genre) {
        Element elem = new Element("genre", EPGDATATYPES.getUri());
        elem.setNamespacePrefix(EPGDATATYPES.getPrefix());
        elem.addAttribute(new Attribute("href", genre));
        return elem;
    }
}
