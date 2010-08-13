package org.atlasapi.feeds.interlinking.validation;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URL;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.common.io.Resources;
import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.Schema;
import com.thaiopensource.validate.SchemaReader;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.Validator;
import com.thaiopensource.validate.rng.CompactSchemaReader;
import com.thaiopensource.validate.rng.RngProperty;

public class InterlinkOutputValidator {

	public boolean validatesAgainstSchema(String xml, OutputStream outputStream) throws Exception {
		URL schemaUrl = null;
		PrintStream out = new PrintStream(outputStream);
		try {
			schemaUrl = Resources.getResource("org/atlasapi/feeds/interlinking/outputting/interlinking.rnc");
		} catch (IllegalArgumentException e) {
			out.println("Could not find schema to validate feed");
			return false;
		}
		SchemaReader schemaReader = CompactSchemaReader.getInstance();
		Schema schema = schemaReader.createSchema(new InputSource(schemaUrl.openStream()), PropertyMap.EMPTY);

		PropertyMapBuilder properties = new PropertyMapBuilder();
		RngProperty.CHECK_ID_IDREF.add(properties);
		PrintingErrorHandler printingErrorHandler = new PrintingErrorHandler(out);
		ValidateProperty.ERROR_HANDLER.put(properties, printingErrorHandler);
		Validator validator = schema.createValidator(properties.toPropertyMap());

		XMLReader xmlReader = XMLReaderFactory.createXMLReader();

		xmlReader.setContentHandler(validator.getContentHandler());
		xmlReader.setDTDHandler(validator.getDTDHandler());

		try {
			xmlReader.parse(new InputSource(new StringReader(xml)));
		} catch (SAXException e) {
			return false;
		}
		return !printingErrorHandler.thereWasAnError;
	}
	 
	 private static class PrintingErrorHandler implements ErrorHandler {

		private boolean thereWasAnError = false;
		private final PrintStream out;
		 
		public PrintingErrorHandler(PrintStream out) {
			this.out = out;
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			printError(exception);
			thereWasAnError = true;
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			printError(exception);
			thereWasAnError = true;
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			printError(exception);
			thereWasAnError = true;
		}

		private void printError(SAXParseException exception) {
			out.println(exception + " on line " + exception.getLineNumber() + ":" + exception.getColumnNumber());
		}
	 }
}
