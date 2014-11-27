package org.atlasapi.feeds.youview.upload;


public class YouViewClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public YouViewClientException(String message) {
        super(message);
    }
    
    public YouViewClientException(Exception e) {
        super(e);
    }
    
    public YouViewClientException(String message, Exception e) {
        super(message, e);
    }
}
