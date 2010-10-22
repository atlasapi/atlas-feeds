package org.atlasapi.feeds.radioplayer.outputting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class RadioPlayerReadingGenreMapTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testMap() {
		RadioPlayerCSVReadingGenreMap gm = new RadioPlayerCSVReadingGenreMap("org/atlasapi/feeds/radioplayer/testgenres.csv");
		
		assertThat(
			gm.map(ImmutableSet.of("http://www.bbc.co.uk/programmes/genres/childrens/entertainmentandcomedy")), 
			is(
				equalTo((Set<List<String>>)
						ImmutableSet.of((List<String>)ImmutableList.of("urn:tva:metadata:cs:ContentCS:2007:3.5","urn:tva:metadata:cs:IntendedAudienceCS:2005:4.2.1"))
				)
			)
		);
		
		assertThat(
			gm.map(ImmutableSet.of("http://www.bbc.co.uk/programmes/genres/animation")), 
			is(
				equalTo((Set<List<String>>)
						ImmutableSet.of((List<String>)ImmutableList.of("urn:tva:metadata:cs:FormatCS:2007:2.3.3"))
				)
			)
		);
	}

	@Test
	public void testFullMapCreates() {
		RadioPlayerCSVReadingGenreMap gm = new RadioPlayerCSVReadingGenreMap(RadioPlayerCSVReadingGenreMap.GENRES_FILE);
	}
}
