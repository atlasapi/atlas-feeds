package org.atlasapi.feeds.interlinking;

import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Playlist;
import org.joda.time.DateTime;

public class PlaylistToInterlinkFeedAdapter {

	public InterlinkFeed fromPlaylist(Playlist playlist) {
		InterlinkFeed feed = new InterlinkFeed(playlist.getCanonicalUri());
		
		// TODO These are dummy entries
		feed.withAuthor(new InterlinkFeedAuthor(playlist.getPublisher().name(), playlist.getPublisher().name()));
		feed.withUpdatedAt(new DateTime());
		
		for (Playlist subPlaylist : playlist.getPlaylists()) {
			if (subPlaylist instanceof Brand) {
				InterlinkBrand brand = fromBrand((Brand) subPlaylist);
				feed.addBrand(brand);
				
				// TODO: When we have series info include it here
				InterlinkSeries series = new InterlinkSeries("dummy-series-" + subPlaylist.getCurie(), 1);
				brand.addSeries(series);
				
				for (Item item : subPlaylist.getItems()) {
					series.addEpisode(fromItem(item));
				}
			}
		}
		return feed;
	}

	private InterlinkEpisode fromItem(Item item) {
		// TODO use correct series position
		return new InterlinkEpisode(item.getCanonicalUri(), 1);
		
	}

	private InterlinkBrand fromBrand(Brand brand) {
		InterlinkBrand linkBrand = new InterlinkBrand(brand.getCanonicalUri());
		linkBrand.withTitle(brand.getTitle());
		return linkBrand;
	}
}
