/* Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.query.content.parser;

import static org.atlasapi.content.criteria.ContentQueryBuilder.query;
import static org.atlasapi.content.criteria.attribute.Attributes.DESCRIPTION_GENRE;
import static org.atlasapi.content.criteria.attribute.Attributes.DESCRIPTION_PUBLISHER;
import static org.atlasapi.content.criteria.attribute.Attributes.DESCRIPTION_TAG;
import static org.atlasapi.content.criteria.attribute.Attributes.LOCATION_AVAILABLE;
import static org.atlasapi.content.criteria.attribute.Attributes.LOCATION_TRANSPORT_TYPE;
import static org.atlasapi.content.criteria.attribute.Attributes.POLICY_AVAILABLE_COUNTRY;
import static org.atlasapi.content.criteria.attribute.Attributes.VERSION_DURATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.ContentQueryBuilder;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Countries;
import org.atlasapi.media.entity.Publisher;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;


public class QueryStringBackedQueryBuilderTest  {
	
	private final QueryStringBackedQueryBuilder builder = new QueryStringBackedQueryBuilder();
	
	@Test
	public void testAvailableCountrySearch() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		
		params = Maps.newHashMap();
		params.put("availableCountries", new String[] { "gb" });
		check(params, query().equalTo(POLICY_AVAILABLE_COUNTRY, Countries.GB.code(), Countries.ALL.code()));
		

		params = Maps.newHashMap();
		params.put("availableCountries", new String[] { "all" });
		check(params, query().equalTo(POLICY_AVAILABLE_COUNTRY, Countries.ALL.code()));
	}
	
	@Test
	public void testAvailability() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("available", new String[] { "true" });
		check(params, query().equalTo(LOCATION_AVAILABLE, true));
		
		params = Maps.newHashMap();
		params.put("available", new String[] { "false" });
		check(params, query().equalTo(LOCATION_AVAILABLE, false));
	}
	
	@Test
	public void testGenreEquality() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();

		params = Maps.newHashMap();
		params.put("genre", new String[] { "bob" });
		check(params, query().equalTo(DESCRIPTION_GENRE, "http://ref.atlasapi.org/genres/atlas/bob"));
	}
	
	@Test
	public void testTagEquality() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		
		params = Maps.newHashMap();
		params.put("tag", new String[] { "bob" });
		check(params, query().equalTo(DESCRIPTION_TAG, "http://ref.atlasapi.org/tags/bob"));
	}
	
	@Test
	public void testDurationEquals() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();

		params = Maps.newHashMap();
		params.put("duration", new String[] { "10" });
		check(params, query().equalTo(VERSION_DURATION, 10));
	}
	
	@Test
	public void testTransportType() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("transportType", new String[] { "link" });
		check(params, query().equalTo(LOCATION_TRANSPORT_TYPE, TransportType.LINK));
	}
	
	@Test
	public void testTitleAndPublisherEqualityDisjunction() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("publisher", new String[] { "bbc.co.uk,channel4.com" });
		check(params, query().isAnEnumIn(DESCRIPTION_PUBLISHER, ImmutableList.<Enum<Publisher>>of(Publisher.BBC, Publisher.C4)));
	}
	
	@Test
	public void testDurationGreaterThan() {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("duration-greaterThan", new String[] {"10"});
		check(params, query().greaterThan(VERSION_DURATION, 10));
	}
	
	@Test
	public void testDurationLessThan() {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("duration-lessThan", new String[] {"10"});
		check(params, query().lessThan(VERSION_DURATION, 10));
	}
	
	@Test
	public void testInvalidAttribute() {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("foo", new String[] {"101"});
		try {
			builder.build(params);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	@Test
	public void testUnknownOperator() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("tag-haltingproblemsolver", new String[] { "est" });
		try {
			builder.build(params);
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException e){
			assertTrue(e.getMessage().contains("Unknown operator"));
		}
	}
	
	private void check(Map<String, String[]> params, ContentQueryBuilder expected) {
		ContentQuery query = builder.build(params);
		assertEquals(expected.build(), query);
	}
}
