package org.atlasapi.feeds.sitemaps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import junit.framework.TestCase;
import nu.xom.Builder;

import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Ignore;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;

@Ignore
public class SiteMapOutputterTest extends TestCase {

    private final TimeMachine clock = new TimeMachine();
	private final SiteMapOutputter outputter = new SiteMapOutputter(ImmutableMap.<Publisher, SiteMapUriGenerator>of(), new DefaultSiteMapUriGenerator(), null, clock);
	
	
	public void testOutputtingASitemap() throws Exception {
		
	    clock.jumpTo(new DateTime(2007, DateTimeConstants.JANUARY, 1, 1, 0, 0, 0));
		Item glee = new Episode("http://www.channel4.com/programmes/grand-designs/1", "c4:grand-designs_1", Publisher.C4);
		glee.setThumbnail("granddesignsThumbnail");
		glee.setTitle("Grand Designs");
		glee.setDescription("Building builds");
		glee.setImage("http://www.channel4.com/assets/programmes/images/grand-designs/series-7/9867d8e4-1c2e-422c-a99d-96257bd0e4ae_625x352.jpg");
		Brand brand = new Brand("http://www.channel4.com/programmes/grand-designs", "c4:grand-designs", Publisher.C4);
		brand.setTitle("Grand Designs");
		((Episode)glee).setContainer(brand);
		Version version = new Version();
		Encoding encoding = new Encoding();
		Location location = new Location();
		location.setUri("http://www.channel4.com/programmes/grand-designs/#3121617");
		Policy policy = new Policy();
		policy.setAvailabilityEnd(new DateTime(2007, 7, 16, 19, 20, 30, 0, DateTimeZone.forOffsetHours(8)));
		policy.addAvailableCountry(Countries.GB);
		policy.addAvailableCountry(Countries.IT);
		location.setPolicy(policy);
		location.setTransportType(TransportType.LINK);
		encoding.addAvailableAt(location);
		version.addManifestedAs(encoding);
		glee.addVersion(version);
		
		assertThat(output(ImmutableList.<Content>of(glee)), is(expectedFeed("sitemap-a.xml")));
	}
	
	public void testThatTheTestDataValidates() throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		// set to false because we are using external xsds
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		
		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		try {
			factory.setSchema(schemaFactory.newSchema(
					new Source[] {
							new StreamSource(
									"https://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd"),
							new StreamSource(
									"https://www.google.com/schemas/sitemap-video/1.1/sitemap-video.xsd"),
							new StreamSource(
									"https://www.sitemaps.org/schemas/sitemap/0.9/siteindex.xsd") }));
		} catch (Exception e){
			if(e.getCause() instanceof SSLHandshakeException){
				System.out.print("WARNING: Website could not be contacted. testThatTheTestDataValidates test was ignored.");
				e.printStackTrace();
				return;
			} else {
				throw e;
			}
		}
		SAXParser parser = factory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		
		reader.setErrorHandler(new ErrorHandler() {
			
			@Override
			public void warning(SAXParseException e) throws SAXException {
				throw e;
			}
			
			@Override
			public void fatalError(SAXParseException e) throws SAXException {
				throw e;
			}
			
			@Override
			public void error(SAXParseException e) throws SAXException {
				throw e;
			}
		});
		Builder builder = new Builder(reader);
		builder.build(new StringReader(expectedFeed("sitemap-a.xml")));
	}
	
	private String output(List<Content> items) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		outputter.output(ImmutableMap.<ParentRef, Container>of(), items, out);
		return out.toString(Charsets.UTF_8.toString());
	}

	private String expectedFeed(String filename) throws IOException {
		return Resources.toString(Resources.getResource("org/atlasapi/feeds/sitemaps/" + filename), Charsets.UTF_8);
	}	
}
