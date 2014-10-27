package org.atlasapi.feeds.youview;

import java.util.Set;

import org.atlasapi.media.entity.Content;

import com.google.common.collect.Sets;


public class UriBasedContentPermit implements ContentPermit {

    private final Set<String> seen = Sets.newHashSet();

    @Override
    public boolean isPermitted(Content content) {
        return seen.add(content.getCanonicalUri());
    }

    @Override
    public void reset() {
        seen.clear();
    }
}
