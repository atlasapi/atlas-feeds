package org.atlasapi.feeds.radioplayer.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import java.io.InputStream;

import nu.xom.ValidityException;

import org.atlasapi.feeds.xml.XMLValidator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class XMLValidatorTest {
	
	@Test
	public void createsWithoutAdditionalSchemas() throws Exception {
			XMLValidator.forSchemas(ImmutableSet.<InputStream>of());
	}
	
	private static XMLValidator validator;
	
	@BeforeClass
	public static void setup() throws Exception {
		validator = XMLValidator.forSchemas(ImmutableSet.of(
		        Resources.getResource("org/atlasapi/feeds/radioplayer/xml.xsd").openStream(),
		        Resources.getResource("org/atlasapi/feeds/radioplayer/epgDataTypes_11.xsd").openStream(),
		        Resources.getResource("org/atlasapi/feeds/radioplayer/rpDataTypes_11.xsd").openStream(),
				Resources.getResource("org/atlasapi/feeds/radioplayer/epgSI_11.xsd").openStream(),
				Resources.getResource("org/atlasapi/feeds/radioplayer/epgSchedule_11.xsd").openStream()
		));
	}

	@Test
	public void validateSuccessfully() {
		try {
			InputStream in = Resources.getResource("org/atlasapi/feeds/radioplayer/basicPIFeedTest.xml").openStream();

			assertThat(validator.validate(in), is(true));

		} catch (Exception e) {
			System.err.println(e);
			fail(e.getMessage());
		} 

	}
	
	@Test(expected=ValidityException.class)
	public void throwsValidityExceptionForInvalidFeed() throws Exception {

		InputStream in = Resources.getResource("org/atlasapi/feeds/radioplayer/invalidPIFeedTest.xml").openStream();
		
		assertThat(validator.validate(in), is(true));
	}
	
	@Test(expected=ValidityException.class)
	public void throwsValidityExceptionForUnparseableFeed() throws Exception {

		InputStream in = Resources.getResource("org/atlasapi/feeds/radioplayer/invalidPIFeedTest.xml").openStream();
		
		assertThat(validator.validate(in), is(true));
	}
}
