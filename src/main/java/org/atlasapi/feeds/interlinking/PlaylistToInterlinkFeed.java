package org.atlasapi.feeds.interlinking;

import java.util.List;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

public interface PlaylistToInterlinkFeed {

    public InterlinkFeed fromContent(String id, Publisher publisher, DateTime from, DateTime to, List<Content> brands);
}