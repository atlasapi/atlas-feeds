package org.atlasapi.feeds.interlinking;

import java.util.List;
import java.util.Map;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.DateTime;

public class DelegatingPlaylistToInterlinkAdapter implements PlaylistToInterlinkFeed {
    
    private final Map<Publisher, PlaylistToInterlinkFeed> delegates;
    private final PlaylistToInterlinkFeed fallback;

    public DelegatingPlaylistToInterlinkAdapter(Map<Publisher, PlaylistToInterlinkFeed> delegates, PlaylistToInterlinkFeed fallback) {
        this.delegates = delegates;
        this.fallback = fallback;
    }

    @Override
    public InterlinkFeed fromContent(String id, Publisher publisher, DateTime from, DateTime to, List<Content> brands) {
        if (publisher != null && delegates.containsKey(publisher)) {
            return delegates.get(publisher).fromContent(id, publisher, from, to, brands);
        }
        
        return fallback.fromContent(id, publisher, from, to, brands);
    }
}
