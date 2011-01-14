package org.atlasapi.feeds.radioplayer.outputting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import nu.xom.ValidityException;

import org.atlasapi.feeds.radioplayer.upload.RadioPlayerXMLValidator;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class RadioPlayerXMLValidatorTest {
	
	@Test
	public void createsWithoutAdditionalSchemas() throws Exception {
			RadioPlayerXMLValidator validator = RadioPlayerXMLValidator.forSchemas(ImmutableSet.<InputStream>of());
			validator.validate(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><schema/>".getBytes()));
	}
	
	private RadioPlayerXMLValidator validator;
	
	@Before
	public void setup() throws Exception {
		validator = RadioPlayerXMLValidator.forSchemas(ImmutableSet.of(
				Resources.getResource("org/atlasapi/feeds/radioplayer/epgSI_10.xsd").openStream(),
				Resources.getResource("org/atlasapi/feeds/radioplayer/epgSchedule_10.xsd").openStream()
		));
	}

	@Test
	public void validateSuccessfully() {
		try {
			InputStream in = Resources.getResource("org/atlasapi/feeds/radioplayer/basicPIFeedTest.xml").openStream();
//			File parent = new File("/");
//			for (File sifile : parent.listFiles()) {
//				try{
//				InputStream in = new FileInputStream(sifile);
//				
				assertThat(validator.validate(in), is(true));
//				}catch(Exception e) {
//					System.out.println(sifile.getName() + ": " + e);
//				}
//			}

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
