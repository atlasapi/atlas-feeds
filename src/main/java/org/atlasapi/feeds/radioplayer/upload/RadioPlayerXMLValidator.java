package org.atlasapi.feeds.radioplayer.upload;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import nu.xom.Builder;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class RadioPlayerXMLValidator {

    private final SAXParserFactory factory;

    public RadioPlayerXMLValidator(SAXParserFactory factory) {
        this.factory = factory;
    }

    public boolean validate(InputStream input) throws ValidityException {
        try {
            buildBuilder().build(input);
            return true;
        } catch (ParsingException e) {
            throw new ValidityException(String.format("Parsing exception whilst validating input: %s [%d:%d]", e.getLocalizedMessage(), e.getLineNumber(), e.getColumnNumber()), e);
        } catch (IOException e) {
            throw new ValidityException("IO Exception whilst validating input", e);
        } catch (ParserConfigurationException pce) {
            throw new ValidityException("Couldn't build validator", pce);
        } catch (SAXException saxe) {
            throw new ValidityException("Couldn't build validator", saxe);
        }
    }

    private Builder buildBuilder() throws ParserConfigurationException, SAXException {
        SAXParser parser = factory.newSAXParser();

        XMLReader reader = parser.getXMLReader();

        reader.setErrorHandler(new ErrorHandler() {

            @Override
            public void error(SAXParseException spe) throws SAXException {
                throw spe;
            }

            @Override
            public void fatalError(SAXParseException spe) throws SAXException {
                throw spe;
            }

            @Override
            public void warning(SAXParseException spe) throws SAXException {
                throw spe;
            }

        });

        return new Builder(reader);
    }

    public static RadioPlayerXMLValidator forSchemas(Iterable<InputStream> schemas) throws SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        if (!Iterables.isEmpty(schemas)) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = schemaFactory.newSchema(Iterables.toArray(Iterables.transform(schemas, new Function<InputStream, Source>() {
                @Override
                public Source apply(InputStream input) {
                    return new StreamSource(input);
                }
            }), Source.class));
            factory.setSchema(schema);
        }

        return new RadioPlayerXMLValidator(factory);
    }

}
