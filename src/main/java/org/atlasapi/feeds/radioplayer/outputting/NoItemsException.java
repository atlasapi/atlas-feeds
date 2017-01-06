package org.atlasapi.feeds.radioplayer.outputting;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedSpec;

public class NoItemsException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final RadioPlayerFeedSpec spec;

    public NoItemsException(RadioPlayerFeedSpec spec) {
        this.spec = spec;
    }

    @Override
    public String getMessage() {
        return String.format("No items to createDefault feed %s", spec);
    }

}
