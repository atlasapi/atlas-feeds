package org.atlasapi.feeds.interlinking;

import java.util.Set;

import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.collect.Sets;

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
		
		for (Broadcast broadcast : broadcasts(item)) {
			episode.addBroadcast(fromBroadcast(broadcast));
		}
		
		Location linkLocation = firstLinkLocation(item);

		if (linkLocation != null) {
			episode.addOnDemand(fromLocation(linkLocation));
		}
		
		return episode;
	}

	private InterlinkBrand fromBrand(Brand brand) {
		InterlinkBrand linkBrand = new InterlinkBrand(brand.getCanonicalUri());
		linkBrand.withTitle(brand.getTitle());
		return linkBrand;
	}
	
	private InterlinkOnDemand fromLocation(Location linkLocation) {
		return new InterlinkOnDemand(linkLocation.getUri());
	}

	private InterlinkBroadcast fromBroadcast(Broadcast broadcast) {
		// use a generated id for now until ids are in the model
		String generatedId = broadcast.getBroadcastOn() + "-" + broadcast.getTransmissionTime().getMillis();
		InterlinkBroadcast linkBroadcast = new InterlinkBroadcast(generatedId)
			.withDuration(toDuration(broadcast.getBroadcastDuration()))
			.withBroadcastStart(broadcast.getTransmissionTime());
		return linkBroadcast;
	}
	
	private static Set<Broadcast> broadcasts(Item item) {
		Set<Broadcast> broadcasts = Sets.newHashSet();
		for (Version version : item.getVersions()) {
			for (Broadcast broadcast : version.getBroadcasts()) {
				broadcasts.add(broadcast);
			}
		}
		return broadcasts;
	}

	private static Location firstLinkLocation(Item item) {
		for (Version version : item.getVersions()) {
			for (Encoding encoding : version.getManifestedAs()) {
				for (Location location : encoding.getAvailableAt()) {
					if (TransportType.LINK.equals(location.getTransportType())) {
						return location;
					}
				}
			}
		}
		return null;
	}

	private static Integer itemIndexFrom(Item item) {
		if (item instanceof Episode) {
			return ((Episode) item).getEpisodeNumber();
		}
		return null;
	}

	private static Duration toDuration(Integer seconds) {
		if (seconds != null) {
			return Duration.standardSeconds(seconds);
		}
		return null;
	}
}
