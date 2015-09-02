package org.atlasapi.feeds.youview.payload;


public class PayloadGenerationException extends Exception {

    private static final long serialVersionUID = 1L;

    public PayloadGenerationException(String message) {
        super(message);
    }

    public PayloadGenerationException(Exception e) {
        super(e);
    }

    public PayloadGenerationException(String message, Exception e) {
        super(message, e);
    }
}
