package org.atlasapi.feeds.radioplayer.outputting;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nu.xom.Builder;
import nu.xom.ParsingException;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.media.entity.Item;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class RadioPlayerXMLValidationTest {

	private RadioPlayerXMLOutputter outputter = new RadioPlayerProgrammeInformationOutputter();

	private ByteArrayOutputStream output(List<Item> items) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		outputter.output(new DateTime(2010, 9, 6, 0, 0, 0, 0, DateTimeZone.UTC),
				new RadioPlayerService(502, "radio2").withDabServiceId("e1_ce15_c222_0"), items, out);
		return out;
	}

	@Test
	public void validate() {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(true);

			SAXParser parser = factory.newSAXParser();

			XMLReader reader = parser.getXMLReader();
//			reader.setErrorHandler(new SimpleErrorHandler());

			Builder builder = new Builder(reader);

			ByteArrayOutputStream outputBytes = output(RadioPlayerProgrammeInformationOutputterTest.buildItems());
			System.out.println(outputBytes.toString());
			InputStream in = new ByteArrayInputStream(outputBytes.toByteArray());

			builder.build(in);

		} catch (SAXException e) {
			fail(e.getMessage());
		} catch (ParsingException e) {
			fail(String.format("Parsing failed, not schema valid: %s (%d,%d)", e.getMessage(), e.getLineNumber(), e.getColumnNumber()));
		} catch (IOException e) {
			fail(e.getMessage());
		} catch (ParserConfigurationException e) {
			fail(e.getMessage());
		}

	}

}
