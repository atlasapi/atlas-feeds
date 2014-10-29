package org.atlasapi.feeds.youview;


public class NoChannelFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_MESSAGE = "Unable to resolve a Channel for uri %s";

    public NoChannelFoundException(String channelUri) {
        super(String.format(ERROR_MESSAGE, channelUri));
    }
}
