package org.atlasapi.feeds.youview.upload;


public class RemoteCheckException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RemoteCheckException(String message) {
        super(message);
    }
    
    public RemoteCheckException(Exception e) {
        super(e);
    }
    
    public RemoteCheckException(String message, Exception e) {
        super(message, e);
    }
}
