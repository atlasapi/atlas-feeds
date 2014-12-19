package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Content;


public class UnexpectedContentTypeException extends RuntimeException {

    private static final long serialVersionUID = 5742882280319040868L;

    public <C extends Content> UnexpectedContentTypeException(C type) {
        super("Unknown sub type of content " + type.getClass().getCanonicalName());
    }
    
    public <C extends Content> UnexpectedContentTypeException(Class<? extends Content> expectedType, C actual) {
        super(String.format("Expected %s to be %s but was %s", actual.getCanonicalUri(), expectedType.getCanonicalName(), actual.getClass().getCanonicalName()));
    }
}
