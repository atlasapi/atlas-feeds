package org.atlasapi.feeds.interlinking;

import java.util.Map;

import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Publisher;

public class DelegatingPlaylistToInterlinkAdapter implements PlaylistToInterlinkFeed {
    
    private final Map<Publisher, PlaylistToInterlinkFeed> delegates;
    private final PlaylistToInterlinkFeed fallback;

    public DelegatingPlaylistToInterlinkAdapter(Map<Publisher, PlaylistToInterlinkFeed> delegates, PlaylistToInterlinkFeed fallback) {
        this.delegates = delegates;
        this.fallback = fallback;
    }

    @Override
    public InterlinkFeed fromPlaylist(Playlist playlist) {
        if (playlist.getPublisher() != null && delegates.containsKey(playlist.getPublisher())) {
            return delegates.get(playlist.getPublisher()).fromPlaylist(playlist);
        }
        
        return fallback.fromPlaylist(playlist);
    }
}
