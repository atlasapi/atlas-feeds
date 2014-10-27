package org.atlasapi.feeds.youview;


public class NoSuchChannelAliasException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_MESSAGE = "No Alias found with namespace %s";

    public NoSuchChannelAliasException(String namespace) {
        super(String.format(ERROR_MESSAGE, namespace));
    }
}
