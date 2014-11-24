package org.atlasapi.feeds.youview.upload;

import static org.atlasapi.feeds.youview.upload.ValidationErrorType.ERROR;
import static org.atlasapi.feeds.youview.upload.ValidationErrorType.FATAL_ERROR;
import static org.atlasapi.feeds.youview.upload.ValidationErrorType.WARNING;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;


public class JaxbErrorHandler implements ErrorHandler {

    private final Multimap<ValidationErrorType, Exception> errorMap;
    
    public JaxbErrorHandler() {
        this.errorMap = HashMultimap.create();
    }
    
    public void reset() {
        errorMap.clear();
    }
    
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        errorMap.put(WARNING, exception);
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        errorMap.put(ERROR, exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        errorMap.put(FATAL_ERROR, exception);
    }

    public Multimap<ValidationErrorType, Exception> errors() {
        return ImmutableMultimap.copyOf(errorMap);
    }
}
