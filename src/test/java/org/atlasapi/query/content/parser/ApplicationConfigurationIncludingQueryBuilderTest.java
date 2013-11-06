package org.atlasapi.query.content.parser;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.atlasapi.application.Application;
import org.atlasapi.application.ApplicationCredentials;
import org.atlasapi.application.auth.ApiKeySourcesFetcher;
import org.atlasapi.application.auth.ApplicationSourcesFetcher;
import org.atlasapi.application.auth.InvalidApiKeyException;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.media.common.Id;
import org.atlasapi.persistence.application.ApplicationStore;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Optional;
import com.metabroadcast.common.servlet.StubHttpServletRequest;

public class ApplicationConfigurationIncludingQueryBuilderTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testBuild() throws InvalidApiKeyException {
		final String testApiKey = "testKey";
		final Application testApp = Application.builder()
		        .withId(Id.valueOf(5000))
		        .withSlug("testSlug")
		        .withCredentials(ApplicationCredentials.builder().withApiKey(testApiKey).build())
		        .build();
		
		Mockery context = new Mockery();
		final ApplicationStore reader = context.mock(ApplicationStore.class);
		
		
		context.checking(new Expectations(){{
			oneOf(reader).applicationForKey(testApiKey);
			will(returnValue(Optional.of(testApp)));
		}});
		
		ApplicationSourcesFetcher sourcesFetcher = new ApiKeySourcesFetcher(reader);
		ApplicationConfigurationIncludingQueryBuilder builder = new ApplicationConfigurationIncludingQueryBuilder(new QueryStringBackedQueryBuilder(), sourcesFetcher) ;

		HttpServletRequest request = new StubHttpServletRequest().withParam("tag", "East").withParam("apiKey", testApiKey);
		ContentQuery query = builder.build(request);
		
		assertEquals(testApp.getSources(), query.getSources());		
	}

}
