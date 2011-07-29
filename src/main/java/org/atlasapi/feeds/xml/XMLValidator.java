package org.atlasapi.feeds.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import nu.xom.ValidityException;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class XMLValidator {

    private final Schema schema;

    public XMLValidator(Schema schema) {
        this.schema = schema;
    }

    public boolean validate(InputStream input) throws ValidityException {
        return validate(new StreamSource(input));
    }
    
    public boolean validate(Source input) throws ValidityException {
        try {
            schema.newValidator().validate(input);
            return true;
        } catch (IOException e) {
            throw new ValidityException("IO Exception whilst validating input", e);
        } catch (SAXException saxe) {
            throw new ValidityException("SAX Exception whilst validating input", saxe);
        }
    }

    public static XMLValidator forSchemas(Iterable<InputStream> schemas) throws SAXException, ParserConfigurationException {

        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        schemaFactory.setResourceResolver(new LSResourceResolver() {

            @Override
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                //TODO find someway to resolve from resources or use XMLCatalogResolver from xerces?
                return null;
            }
        });

        Schema schema = null;
        if (!Iterables.isEmpty(schemas)) {
            schema = schemaFactory.newSchema(Iterables.toArray(Iterables.transform(schemas, new Function<InputStream, Source>() {
                @Override
                public Source apply(InputStream input) {
                    return new StreamSource(input);
                }
            }), Source.class));
        } else {
            schema = schemaFactory.newSchema();
        }
        
        return new XMLValidator(schema);
    }

}
