package org.atlasapi.feeds.radioplayer.upload.queue;


public class RemoteCheckException extends Exception {

    private static final long serialVersionUID = 6110517278470629564L;

    public RemoteCheckException(String message) {
        super(message);
    }
    
    public RemoteCheckException(Throwable t) {
        super(t);
    }
    
    public RemoteCheckException(String message, Throwable t) {
        super(message, t);
    }
}
