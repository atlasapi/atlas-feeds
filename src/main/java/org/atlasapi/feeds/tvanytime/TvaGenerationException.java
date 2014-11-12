package org.atlasapi.feeds.tvanytime;


public class TvaGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TvaGenerationException(String message) {
        super(message);
    }
    
    public TvaGenerationException(Exception e) {
        super(e);
    }
    
    public TvaGenerationException(String message, Exception e) {
        super(message, e);
    }
}
