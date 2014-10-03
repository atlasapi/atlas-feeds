package org.atlasapi.feeds.sitemaps;

import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Location;

import com.google.common.base.Optional;


public class DefaultSiteMapUriGenerator implements SiteMapUriGenerator {

    @Override
    public Optional<String> playerPageUriForClip(Content content, Clip clip, Location location) {
        return Optional.fromNullable(clip.getCanonicalUri());
    }

    @Override
    public Optional<String> videoUriForClip(Clip clip, Location location) {
        return Optional.absent();
    }

    @Override
    public Optional<String> playerPageUriForContent(Content content, Location location) {
        return Optional.fromNullable(location.getUri());
    }

    @Override
    public Optional<String> videoUriForContent(Content content, Location location) {
        return Optional.absent();
    }

}
