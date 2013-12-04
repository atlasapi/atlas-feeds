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

    private final RadioPlayerGenreMap GENRES;
    protected static final XMLNamespace EPGDATATYPES = new XMLNamespace("epg", "http://www.radioplayer.co.uk/schemas/11/epgDataTypes");
    private final List<String> genreTypes = ImmutableList.of("main", "secondary", "secondary", "secondary");
    private final Ordering<String> genreTypeComparator = Ordering.explicit(genreTypes.subList(0, 2));

    public RadioPlayerGenreElementCreator(RadioPlayerGenreMap radioPlayerGenreMap) {
        this.GENRES = radioPlayerGenreMap;
    }

    public List<Element> genreElementsFor(Item item) {

        Iterable<List<String>> mappedGenres = GENRES.map(item.getGenres());
        
        Set<String> genres = Sets.newHashSet();
        for (List<String> genreList : mappedGenres) {
            genres = Sets.union(genres, Sets.newCopyOnWriteArraySet(genreList));
        }

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
