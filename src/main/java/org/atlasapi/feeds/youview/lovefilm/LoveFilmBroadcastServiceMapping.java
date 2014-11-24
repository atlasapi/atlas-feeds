package org.atlasapi.feeds.youview.lovefilm;

import org.atlasapi.feeds.youview.services.BroadcastServiceMapping;


public class LoveFilmBroadcastServiceMapping implements BroadcastServiceMapping {

    @Override
    public Iterable<String> youviewServiceIdFor(String bbcServiceId) {
        throw new UnsupportedOperationException("LOVEFiLM catalog does not contain broadcast data");
    }

}
