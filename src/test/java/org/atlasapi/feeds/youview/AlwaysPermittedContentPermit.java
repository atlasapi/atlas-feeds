package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Content;


public class AlwaysPermittedContentPermit implements ContentPermit {

    @Override
    public boolean isPermitted(Content content) {
        return true;
    }

    @Override
    public void reset() {
    }

}
