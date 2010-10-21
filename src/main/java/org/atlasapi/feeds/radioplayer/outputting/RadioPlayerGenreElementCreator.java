package org.atlasapi.feeds.radioplayer.outputting;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Element;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.entity.Item;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class RadioPlayerGenreElementCreator {

	private final RadioPlayerGenreMap GENRES = new RadioPlayerGenreMap();
	protected static final XMLNamespace EPGDATATYPES = new XMLNamespace("epg", "http://www.radioplayer.co.uk/schemas/10/epgDataTypes");
	private final List<String> genreTypes = ImmutableList.of("main", "secondary", "tertiary", "quarternary");
	private final Ordering<String> genreTypeComparator = Ordering.explicit(genreTypes);
	
	public List<Element> genreElementsFor(Item item) {
		//Get the genres without the current ones.
		Set<String> mappedGenres = Sets.difference(GENRES.mapRecognised(item.getGenres()), item.getGenres());
		if (Iterables.isEmpty(mappedGenres)) {
			return ImmutableList.of();
		}
		
		//Split the mapped strings up into separate genres.
		Iterable<List<String>> splitGenres = Iterables.transform(mappedGenres, new Function<String, List<String>>() {
			@Override
			public List<String> apply(String from) {
				return ImmutableList.copyOf(from.split("\\|"));
			}
		});
			
		final Map<String, String> finalGenres = Maps.newHashMap();
		
		//Create map of genre to type backwards, so most significant genre type definition is left in map.
		for (int i = genreTypes.size()-1; i >= 0; i--) {
			for (List<String> genreGroup : splitGenres) {
				if (genreGroup.size() > i) {
					finalGenres.put(genreGroup.get(i), genreTypes.get(i));
				}
			}
		}
		
		//Order keys by type.
		Iterable<String> orderedKeys = Ordering.from(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return genreTypeComparator.compare(finalGenres.get(o1), finalGenres.get(o2));
			}
		}).sortedCopy(finalGenres.keySet());
		
		//Create the actual genre elements
		ImmutableList<Element> genreElements = ImmutableList.copyOf(Iterables.transform(orderedKeys, new Function<String, Element>(){
			@Override
			public Element apply(String genre) {
				return createGenreElement(genre, finalGenres.get(genre));
			}}));
		
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
