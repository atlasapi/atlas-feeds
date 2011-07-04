package org.atlasapi.feeds.interlinking;

import java.util.Iterator;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

public interface PlaylistToInterlinkFeed {

    public InterlinkFeed fromContent(String id, Publisher publisher, DateTime from, DateTime to, Iterator<Content> brands);
    
}