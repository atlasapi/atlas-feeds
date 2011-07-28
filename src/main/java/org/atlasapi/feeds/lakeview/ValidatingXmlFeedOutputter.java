package org.atlasapi.feeds.lakeview;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import nu.xom.Document;
import nu.xom.converters.DOMConverter;

import org.atlasapi.feeds.xml.XMLValidator;

public class ValidatingXmlFeedOutputter implements XmlFeedOutputter {

    private final XmlFeedOutputter delegate;
    private final XMLValidator validator;

    public ValidatingXmlFeedOutputter(XMLValidator validator, XmlFeedOutputter delegate) {
        this.validator = validator;
        this.delegate = delegate;
    }
    
    @Override
    public void outputTo(Document document, OutputStream out) throws IOException {
        if(validator != null) {
            try {
                validator.validate(new DOMSource(DOMConverter.convert(document, DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation())));
            } catch (Exception e) {
                throw new IOException("Exception whilst validating", e);
            }
        }
        delegate.outputTo(document, out);
    }
    
}
