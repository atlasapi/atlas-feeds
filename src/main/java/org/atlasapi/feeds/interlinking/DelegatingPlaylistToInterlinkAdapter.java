package org.atlasapi.feeds.interlinking;

import java.util.List;
import java.util.Map;

import org.atlasapi.media.entity.Brand;
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
    public InterlinkFeed fromBrands(String id, Publisher publisher, DateTime from, DateTime to, List<Brand> brands) {
        if (publisher != null && delegates.containsKey(publisher)) {
            return delegates.get(publisher).fromBrands(id, publisher, from, to, brands);
        }
        
        return fallback.fromBrands(id, publisher, from, to, brands);
    }
}
