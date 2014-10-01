package org.atlasapi.feeds.sitemaps;

import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Location;

import com.google.common.base.Optional;


public interface SiteMapUriGenerator {
    
    Optional<String> playerPageUriForClip(Content content, Clip clip, Location location);
    Optional<String> videoUriForClip(Clip clip, Location location);

    Optional<String> playerPageUriForContent(Content content, Location location);
    Optional<String> videoUriForContent(Content content, Location location);
    
}
