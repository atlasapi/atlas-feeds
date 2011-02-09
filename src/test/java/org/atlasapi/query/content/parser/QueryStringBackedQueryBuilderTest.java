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
import static org.atlasapi.content.criteria.attribute.Attributes.BROADCAST_TRANSMISSION_END_TIME;
import static org.atlasapi.content.criteria.attribute.Attributes.BROADCAST_TRANSMISSION_TIME;
import static org.atlasapi.content.criteria.attribute.Attributes.DESCRIPTION_GENRE;
import static org.atlasapi.content.criteria.attribute.Attributes.DESCRIPTION_PUBLISHER;
import static org.atlasapi.content.criteria.attribute.Attributes.DESCRIPTION_TAG;
import static org.atlasapi.content.criteria.attribute.Attributes.DESCRIPTION_TITLE;
import static org.atlasapi.content.criteria.attribute.Attributes.EPISODE_POSITION;
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
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Countries;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.metabroadcast.common.time.DateTimeZones;


public class QueryStringBackedQueryBuilderTest  {
	
	private final QueryStringBackedQueryBuilder builder = new QueryStringBackedQueryBuilder();
	
	@Test
	public void testTitleEquality() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		
		params = Maps.newHashMap();
		params.put("title", new String[] { "bob" });
		check(params, query().searchFor(DESCRIPTION_TITLE, "bob"));
	}
	
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
	public void testTitleBeginning() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("title-beginning", new String[] { "a" });
		check(params, query().beginning(DESCRIPTION_TITLE, "a"));
	}
	
	@Test
	public void testDurationEquals() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();

		params = Maps.newHashMap();
		params.put("duration", new String[] { "10" });
		check(params, query().equalTo(VERSION_DURATION, 10));
	}
	
	@Test
	public void testTitleAndPublisherEqualityConjunction() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("title", new String[] { "bob" });
		params.put("publisher", new String[] { "bbc.co.uk" });
		check(params, query().equalTo(DESCRIPTION_PUBLISHER, Publisher.BBC).searchFor(DESCRIPTION_TITLE, "bob"));
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
	public void testTitleContainsConjunction() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("title-search", new String[] { "bob", "vic" });
		check(params, query().searchFor(DESCRIPTION_TITLE, "bob").searchFor(DESCRIPTION_TITLE, "vic"));
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
	public void testTransmittedAfter() {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("transmissionTime-after", new String[] {"1"});
		check(params, query().after(BROADCAST_TRANSMISSION_TIME, new DateTime(1000L, DateTimeZones.UTC)));
	}
	
	@Test
	public void testTransmittedBefore() {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("transmissionTime-before", new String[] {"1"});
		check(params, query().before(BROADCAST_TRANSMISSION_TIME, new DateTime(1000L, DateTimeZones.UTC)));
	}
	
	@Test
	public void testTransmissionTimeEquals() throws Exception {
		DateTime when = new DateTime(2000, DateTimeZones.UTC);
		Map<String, String[]> params = Maps.newHashMap();
		params.put("transmissionTime", new String[] { String.valueOf(when.getMillis() / 1000L) });
		check(params, query().before(BROADCAST_TRANSMISSION_TIME, when.plusSeconds(1)).after(BROADCAST_TRANSMISSION_END_TIME, when));
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
		params.put("title-haltingproblemsolver", new String[] { "est" });
		try {
			builder.build(params);
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException e){
			assertTrue(e.getMessage().contains("Unknown operator"));
		}
	}
	
	@Test
	public void testIgnoringParameters() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("title", new String[] { "bob" });
		params.put("foo", new String[] { "bob2" });
		
		ContentQuery query = 
			builder
			.withIgnoreParams("foo")
			.build(params);
		
		assertEquals(query().searchFor(DESCRIPTION_TITLE, "bob").build(), query);
	}
	
	@Test
	public void testBroadcastQuery() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("transmissionTime-after", new String[] { "10101" });
		
		new QueryStringBackedQueryBuilder(new WebProfileDefaultQueryAttributesSetter());
		check(params, query().after(Attributes.BROADCAST_TRANSMISSION_TIME, new DateTime(10101 * 1000, DateTimeZones.UTC)));
	}
	
	@Test
	public void testAliases() throws Exception {
		Map<String, String[]> params = Maps.newHashMap();
		params.put("title", new String[] { "bob" });
		check(params, query().searchFor(DESCRIPTION_TITLE, "bob"));
		
		params = Maps.newHashMap();
		params.put("available", new String[] { "true" });
		check(params, query().equalTo(LOCATION_AVAILABLE, true));
		
		params = Maps.newHashMap();
		params.put("position", new String[] { "10" });
		check(params, query().equalTo(EPISODE_POSITION, 10));
	}
	
	@Test
	public void testDefaultSetting() throws Exception {
		QueryStringBackedQueryBuilder builderWithDefaults = new QueryStringBackedQueryBuilder(new WebProfileDefaultQueryAttributesSetter());

		Map<String, String[]> params = Maps.newHashMap();
		params.put("title", new String[] { "bob" });
		
		assertEquals(query().searchFor(DESCRIPTION_TITLE, "bob").equalTo(LOCATION_AVAILABLE, true).build(), builderWithDefaults.build(params));
	
		params = Maps.newHashMap();
		params.put("title", new String[] { "bob" });
		params.put("available", new String[] { "false" });
		
		assertEquals(query().searchFor(DESCRIPTION_TITLE, "bob").equalTo(LOCATION_AVAILABLE, false).build(), builderWithDefaults.build(params));

		params = Maps.newHashMap();
		params.put("title", new String[] { "bob" });
		params.put("available", new String[] { "any" });
		
		assertEquals(query().searchFor(DESCRIPTION_TITLE, "bob").build(), builderWithDefaults.build(params));
	}
	
	private void check(Map<String, String[]> params, ContentQueryBuilder expected) {
		ContentQuery query = builder.build(params);
		assertEquals(expected.build(), query);
	}
}
