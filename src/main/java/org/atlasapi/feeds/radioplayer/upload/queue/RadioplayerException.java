package org.atlasapi.feeds.radioplayer.upload.queue;


public class RadioplayerException extends RuntimeException {

    private static final long serialVersionUID = 241869801331256527L;

    public RadioplayerException(String message) {
        super(message);
    }
    
    public RadioplayerException(Throwable t) {
        super(t);
    }
    
    public RadioplayerException(String message, Throwable t) {
        super(message, t);
    }
}
