package org.atlasapi.query.content.parser;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.atlasapi.application.Application;
import org.atlasapi.application.ApplicationCredentials;
import org.atlasapi.application.persistence.ApplicationReader;
import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.application.query.IpCheckingApiKeyConfigurationFetcher;
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
		final Application testApp = Application.application("testSlug").withCredentials(new ApplicationCredentials(testApiKey)).build();
		
		Mockery context = new Mockery();
		final ApplicationReader reader = context.mock(ApplicationReader.class);
		
		
		context.checking(new Expectations(){{
			oneOf(reader).applicationForKey(testApiKey);
			will(returnValue(Optional.of(testApp)));
		}});
		
		ApplicationConfigurationFetcher configFetcher = new IpCheckingApiKeyConfigurationFetcher(reader);
		ApplicationConfigurationIncludingQueryBuilder builder = new ApplicationConfigurationIncludingQueryBuilder(new QueryStringBackedQueryBuilder(), configFetcher) ;

		HttpServletRequest request = new StubHttpServletRequest().withParam("tag", "East").withParam("apiKey", testApiKey);
		ContentQuery query = builder.build(request);
		
		assertEquals(testApp.getConfiguration(), query.getConfiguration());		
	}

}
