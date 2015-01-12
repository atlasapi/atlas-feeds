package org.atlasapi.feeds.tvanytime.granular;


public class TvaValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TvaValidationException(String message) {
        super(message);
    }

    public TvaValidationException(Exception e) {
        super(e);
    }
    
    public TvaValidationException(String message, Exception e) {
        super(message, e);
    }
}
