package org.atlasapi.feeds.interlinking;

import org.atlasapi.media.entity.Playlist;

public interface PlaylistToInterlinkFeed {

    public InterlinkFeed fromPlaylist(Playlist playlist);

}