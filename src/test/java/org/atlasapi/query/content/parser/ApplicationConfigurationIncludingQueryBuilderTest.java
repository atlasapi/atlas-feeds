package org.atlasapi.query.content.parser;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.atlasapi.application.OldApplication;
import org.atlasapi.application.OldApplicationCredentials;
import org.atlasapi.application.OldApplicationStore;
import org.atlasapi.application.query.ApplicationSourcesFetcher;
import org.atlasapi.application.query.ApiKeyConfigurationFetcher;
import org.atlasapi.content.criteria.ContentQuery;
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
	public void testBuild() {
		final String testApiKey = "testKey";
		final OldApplication testApp = OldApplication.application("testSlug").withCredentials(new OldApplicationCredentials(testApiKey)).build();
		
		Mockery context = new Mockery();
		final OldApplicationStore reader = context.mock(OldApplicationStore.class);
		
		
		context.checking(new Expectations(){{
			oneOf(reader).applicationForKey(testApiKey);
			will(returnValue(Optional.of(testApp)));
		}});
		
		ApplicationSourcesFetcher configFetcher = new ApiKeyConfigurationFetcher(reader);
		ApplicationConfigurationIncludingQueryBuilder builder = new ApplicationConfigurationIncludingQueryBuilder(new QueryStringBackedQueryBuilder(), configFetcher) ;

		HttpServletRequest request = new StubHttpServletRequest().withParam("tag", "East").withParam("apiKey", testApiKey);
		ContentQuery query = builder.build(request);
		
		assertEquals(testApp.getConfiguration(), query.getConfiguration());		
	}

}
