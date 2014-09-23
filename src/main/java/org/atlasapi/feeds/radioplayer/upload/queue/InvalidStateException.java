package org.atlasapi.feeds.radioplayer.upload.queue;


public class InvalidStateException extends Exception {

    private static final long serialVersionUID = -404427252798117854L;

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(Throwable t) {
        super(t);
    }

    public InvalidStateException(String message, Throwable t) {
        super(message, t);
    }
}
