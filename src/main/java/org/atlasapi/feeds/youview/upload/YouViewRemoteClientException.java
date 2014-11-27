package org.atlasapi.feeds.youview.upload;


public class YouViewRemoteClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public YouViewRemoteClientException(String message) {
        super(message);
    }
    
    public YouViewRemoteClientException(Exception e) {
        super(e);
    }
    
    public YouViewRemoteClientException(String message, Exception e) {
        super(message, e);
    }
}
