package org.atlasapi.feeds.radioplayer.outputting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import nu.xom.Element;

import org.atlasapi.media.content.Item;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;


public class RadioPlayerGenreElementCreatorTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGenreElementsFor() {
		Item item = new Item();
		item.setGenres(ImmutableSet.<String>of(
				"http://www.bbc.co.uk/programmes/genres/childrens/entertainmentandcomedy",
				"http://www.bbc.co.uk/programmes/genres/childrens/factual",
				"http://ref.atlasapi.org/genres/atlas/factual",
				"http://www.bbc.co.uk/programmes/genres/childrens"
		));
		
		List<Element> genreElems = new RadioPlayerGenreElementCreator(new RadioPlayerCSVReadingGenreMap("radioplayergenres.csv")).genreElementsFor(item);
		
		ImmutableList<String> mappedGenres = ImmutableList.copyOf(Iterables.transform(genreElems, new Function<Element, String>() {
			@Override
			public String apply(Element from) {
				return from.getAttributeValue("href");
			}
		}));
		assertThat(mappedGenres.size(), is(4));
		assertThat(mappedGenres, is(equalTo(ImmutableList.of(
				"urn:tva:metadata:cs:IntendedAudienceCS:2005:4.2.1",
				"urn:tva:metadata:cs:ContentCS:2007:3.1",
				"urn:tva:metadata:cs:ContentCS:2007:3.5",
				"urn:tva:metadata:cs:ContentCS:2007:3.1.3"
		))));
	}

}
