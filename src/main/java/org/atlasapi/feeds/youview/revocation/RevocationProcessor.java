package org.atlasapi.feeds.youview.revocation;

import org.atlasapi.media.entity.Content;


public interface RevocationProcessor {

    void revoke(Content content);
    void unrevoke(Content content);
}
