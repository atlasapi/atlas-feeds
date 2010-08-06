package org.atlasapi.feeds.interlinking;

import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;

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
		InterlinkEpisode episode = new InterlinkEpisode(item.getCanonicalUri(), itemIndexFrom(item))
			.withTitle(item.getTitle());
		
		for (Version version : item.getVersions()) {
			for (Broadcast broadcast : version.getBroadcasts()) {
				episode.addBroadcast(fromBroadcast(broadcast));
			}
		}
		return episode;
	}

	private InterlinkBroadcast fromBroadcast(Broadcast broadcast) {
		// use a generated id for now until ids are in the model
		String generatedId = broadcast.getBroadcastOn() + "-" + broadcast.getTransmissionTime().getMillis();
		InterlinkBroadcast linkBroadcast = new InterlinkBroadcast(generatedId)
			.withDuration(toDuration(broadcast.getBroadcastDuration()))
			.withBroadcastStart(broadcast.getTransmissionTime());
		return linkBroadcast;
		
	}

	private Duration toDuration(Integer seconds) {
		if (seconds != null) {
			return Duration.standardSeconds(seconds);
		}
		return null;
	}

	private Integer itemIndexFrom(Item item) {
		if (item instanceof Episode) {
			return ((Episode) item).getEpisodeNumber();
		}
		return null;
	}

	private InterlinkBrand fromBrand(Brand brand) {
		InterlinkBrand linkBrand = new InterlinkBrand(brand.getCanonicalUri());
		linkBrand.withTitle(brand.getTitle());
		return linkBrand;
	}
}
