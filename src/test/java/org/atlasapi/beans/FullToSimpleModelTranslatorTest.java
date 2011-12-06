package org.atlasapi.beans;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Currency;
import java.util.List;
import java.util.Set;

import org.atlasapi.media.TransportSubType;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Actor;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Restriction;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.entity.simple.ContentQueryResult;
import org.atlasapi.media.entity.simple.Item;
import org.atlasapi.persistence.topic.TopicQueryResolver;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.metabroadcast.common.currency.Price;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.servlet.StubHttpServletRequest;
import com.metabroadcast.common.servlet.StubHttpServletResponse;

@RunWith(JMock.class)
public class FullToSimpleModelTranslatorTest {
    
    private final Mockery context = new Mockery();
    private final ContentResolver contentResolver = context.mock(ContentResolver.class);
    private final AtlasModelWriter xmlOutputter = context.mock(AtlasModelWriter.class);
    private final FullToSimpleModelTranslator translator = new FullToSimpleModelTranslator(xmlOutputter, contentResolver, TopicQueryResolver.NULL_RESOLVER);
    
	private StubHttpServletRequest request;
	private StubHttpServletResponse response;

	@Before
	public void setUp() throws Exception {
		this.request = new StubHttpServletRequest();
		this.response = new StubHttpServletResponse();
	}
	
	@Test
	public void testTranslatesItemsInFullModel() throws Exception {
		
		Set<Object> graph = Sets.newHashSet();
		graph.add(new Episode());
		
		context.checking(new Expectations() {{ 
			one(xmlOutputter).writeTo(with(request), with(response), with(simpleGraph()), with(AtlasModelType.CONTENT));
			ignoring(contentResolver);
		}});
		
		translator.writeTo(request, response, graph, AtlasModelType.CONTENT);
	}

	protected Matcher<Set<Object>> simpleGraph() {
		return new TypeSafeMatcher<Set<Object>> () {

			@Override
			public boolean matchesSafely(Set<Object> beans) {
				if (beans.size() != 1) { return false; }
				Object bean = Iterables.getOnlyElement(beans);
				if (!(bean instanceof ContentQueryResult)) { return false; }
				ContentQueryResult output = (ContentQueryResult) bean;
				if (output.getContents().size() != 1) { return false; }
				return true;
			}

			public void describeTo(Description description) {
				// TODO Auto-generated method stub
			}};
	}
	
	@SuppressWarnings("unchecked")
    public void testCanCreateSimpleItemFromFullItem() throws Exception {
	    
	    context.checking(new Expectations(){{
	        allowing(contentResolver).findByCanonicalUris(with(any(Iterable.class)));will(returnValue(ResolvedContent.builder().build()));
	    }});
		
		org.atlasapi.media.entity.Item fullItem = new org.atlasapi.media.entity.Item();
		Version version = new Version();
		
		Restriction restriction = new Restriction();
		restriction.setRestricted(true);
		restriction.setMessage("adults only");
		version.setRestriction(restriction);
		
		Encoding encoding = new Encoding();
		encoding.setDataContainerFormat(MimeType.VIDEO_3GPP);
		version.addManifestedAs(encoding);
		
		Location location = new Location();
		location.setUri("http://example.com");
		location.setPolicy(new Policy().withRevenueContract(RevenueContract.PAY_TO_BUY).withPrice(new Price(Currency.getInstance("GBP"), 99)).withAvailableCountries(Countries.GB));
		Location embed = new Location();
		embed.setTransportType(TransportType.EMBED);
		embed.setEmbedId("embedId");
		embed.setTransportSubType(TransportSubType.BRIGHTCOVE);
		
		encoding.addAvailableAt(location);
		encoding.addAvailableAt(embed);
		fullItem.addVersion(version);
		fullItem.setTitle("Collings and Herrin");
		
		CrewMember person = Actor.actor("hisID", "Andrew Collings", "Dirt-bag Humperdink", Publisher.BBC);
		fullItem.addPerson(person);
		
		Item simpleItem = translator.simpleItemFrom(fullItem);
		List<org.atlasapi.media.entity.simple.Person> people = simpleItem.getPeople();
		org.atlasapi.media.entity.simple.Person simpleActor = Iterables.getOnlyElement(people);
		assertThat(simpleActor.getCharacter(), is("Dirt-bag Humperdink"));
		assertThat(simpleActor.getName(), is("Andrew Collings"));
		
		Set<org.atlasapi.media.entity.simple.Location> simpleLocations = simpleItem.getLocations();
		assertThat(simpleLocations.size(), is(2));
		org.atlasapi.media.entity.simple.Location simpleLocation = Iterables.getFirst(simpleLocations, null);
		
		assertThat(simpleLocation.getUri(), is("http://example.com"));
		assertThat(simpleLocation.getDataContainerFormat(), is(MimeType.VIDEO_3GPP.toString()));
		assertThat(simpleLocation.getRestriction().getMessage(), is("adults only"));
		assertThat(simpleLocation.getRevenueContract(), is("pay_to_buy"));
		assertThat(simpleLocation.getCurrency(), is("GBP"));
		assertThat(simpleLocation.getPrice(), is(99));
		assertThat(simpleLocation.getAvailableCountries().size(), is(1));
		assertThat(simpleLocation.getAvailableCountries().iterator().next(), is("GB"));
		
		org.atlasapi.media.entity.simple.Location simpleEmbed = Iterables.getLast(simpleLocations, null);
		assertThat(simpleEmbed.getEmbedId(), is("embedId"));
		assertThat(simpleEmbed.getTransportType(), is("embed"));
		assertThat(simpleEmbed.getTransportSubType(), is("brightcove"));
		
		assertThat(simpleItem.getTitle(), is("Collings and Herrin"));
	}
}
