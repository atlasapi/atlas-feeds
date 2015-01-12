package org.atlasapi.feeds.youview.payload;


public class TVAnytimeConverterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TVAnytimeConverterException(String message) {
        super(message);
    }

    public TVAnytimeConverterException(Exception e) {
        super(e);
    }

    public TVAnytimeConverterException(String message, Exception e) {
        super(message, e);
    }
}
