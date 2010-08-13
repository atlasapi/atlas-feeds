package org.atlasapi.feeds.interlinking;

import java.util.Map;
import java.util.Set;

import org.atlasapi.feeds.interlinking.InterlinkBase.Operation;
import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.metabroadcast.common.text.Truncator;

public class PlaylistToInterlinkFeedAdapter implements PlaylistToInterlinkFeed {
    
    protected static final Operation DEFAULT_OPERATION = Operation.STORE;

	private static Map<String, String> channelLookup() {
        Map<String, String> channelLookup = Maps.newHashMap();
        channelLookup.put("http://www.channel4.com", "C4");
        channelLookup.put("http://www.channel4.com/more4", "M4");
        channelLookup.put("http://www.e4.com", "E4");
        return channelLookup;
    }
    public static Map<String, String> CHANNEL_LOOKUP = channelLookup(); 

	private final Truncator summaryTruncator = new Truncator()
		.withMaxLength(90)
		.onlyTruncateAtAWordBoundary()
		.omitTrailingPunctuationWhenTruncated()
		.onlyStartANewSentenceIfTheSentenceIsAtLeastPercentComplete(50).withOmissionMarker("...");
	
	private final Truncator descriptionTruncator = new Truncator()
		.withMaxLength(180)
		.onlyTruncateAtAWordBoundary()
		.omitTrailingPunctuationWhenTruncated()
		.onlyStartANewSentenceIfTheSentenceIsAtLeastPercentComplete(50).withOmissionMarker("...");
	
    public InterlinkFeed fromPlaylist(Playlist playlist) {
        InterlinkFeed feed = new InterlinkFeed(playlist.getCanonicalUri());

        // TODO These are dummy entries
        feed.withAuthor(new InterlinkFeedAuthor(playlist.getPublisher().name(), playlist.getPublisher().name()));
        feed.withUpdatedAt(new DateTime());

        for (Playlist subPlaylist : playlist.getPlaylists()) {
            if (subPlaylist instanceof Brand) {
                InterlinkBrand brand = fromBrand((Brand) subPlaylist);
                feed.addBrand(brand);

                Map<String, InterlinkSeries> seriesLookup = Maps.newHashMap();

                for (Item item : subPlaylist.getItems()) {

                    if (item instanceof Episode) {
                        Episode episode = (Episode) item;
                        Series series = episode.getSeriesSummary();
                        if (series != null) {
                            InterlinkSeries linkSeries = seriesLookup.get(series.getCanonicalUri());
                            if (linkSeries == null) {
                                linkSeries = fromSeries(series);
                                brand.addSeries(linkSeries);
                                seriesLookup.put(series.getCanonicalUri(), linkSeries);
                            }
                            linkSeries.addEpisode(fromItem(episode));
                            continue;
                        }
                    }
                    brand.addEpisodeWithoutASeries(fromItem(item));
                }
            }
        }
        return feed;
    }

    private InterlinkSeries fromSeries(Series series) {
        return new InterlinkSeries(idFrom(series), DEFAULT_OPERATION, series.getSeriesNumber())
        	.withTitle(series.getTitle())
        	.withDescription(toDescription(series))
        	.withLastUpdated(series.getLastUpdated())
        	.withSummary(toSummary(series))
        	.withThumbnail(series.getImage());
    }

    private InterlinkEpisode fromItem(Item item) {
        InterlinkEpisode episode = new InterlinkEpisode(idFrom(item), DEFAULT_OPERATION, itemIndexFrom(item), item.getCanonicalUri())
        	.withTitle(item.getTitle())
        	.withLastUpdated(item.getLastUpdated())
        	.withSummary(toSummary(item))
        	.withDescription(toDescription(item))
        	.withThumbnail(item.getImage());

        for (Broadcast broadcast : broadcasts(item)) {
            InterlinkBroadcast interlinkBroadcast = fromBroadcast(broadcast);
            if (interlinkBroadcast != null) {
                episode.addBroadcast(interlinkBroadcast);
            }
        }

        InterlinkOnDemand onDemand = firstLinkLocation(item);

        if (onDemand != null) {
            episode.addOnDemand(onDemand);
        }

        return episode;
    }

    private InterlinkBrand fromBrand(Brand brand) {
        return new InterlinkBrand(idFrom(brand), DEFAULT_OPERATION)
			.withLastUpdated(brand.getLastUpdated())
        	.withTitle(brand.getTitle())
        	.withDescription(toDescription(brand))
        	.withSummary(toSummary(brand))
        	.withThumbnail(brand.getImage());

    }

  

	protected String idFrom(Content content) {
		return content.getCanonicalUri();
	}

	private String toSummary(Content content) {
    	String description = content.getDescription();
		if (description == null) {
    		return null;
    	}
    	return summaryTruncator.truncate(description);
    }
	
	private String toDescription(Content content) {
    	String description = content.getDescription();
		if (description == null) {
    		return null;
    	}
    	return descriptionTruncator.truncate(description);
    }

    protected InterlinkBroadcast fromBroadcast(Broadcast broadcast) {
        String id = broadcast.getBroadcastOn() + "-" + broadcast.getTransmissionTime().getMillis();
        String service = CHANNEL_LOOKUP.get(broadcast.getBroadcastOn());

        return new InterlinkBroadcast(id, DEFAULT_OPERATION)
    		.withLastUpdated(broadcast.getLastUpdated())
        	.withDuration(toDuration(broadcast.getBroadcastDuration()))
        	.withBroadcastStart(broadcast.getTransmissionTime())
        	.withService(service);
    }

    static Set<Broadcast> broadcasts(Item item) {
        Set<Broadcast> broadcasts = Sets.newHashSet();
        for (Version version : item.getVersions()) {
            for (Broadcast broadcast : version.getBroadcasts()) {
                broadcasts.add(broadcast);
            }
        }
        return broadcasts;
    }

    static InterlinkOnDemand firstLinkLocation(Item item) {
        for (Version version : item.getVersions()) {
            for (Encoding encoding : version.getManifestedAs()) {
                for (Location location : encoding.getAvailableAt()) {
                    if (TransportType.LINK.equals(location.getTransportType())) {
                        return fromLocation(location, version.getDuration());
                    }
                }
            }
        }
        return null;
    }
    
    static InterlinkOnDemand fromLocation(Location linkLocation, int d) {
        Duration duration = new Duration(d*1000);
        
        return new InterlinkOnDemand(linkLocation.getUri(), DEFAULT_OPERATION, linkLocation.getPolicy().getAvailabilityStart(), linkLocation.getPolicy().getAvailabilityEnd(), duration)
            .withLastUpdated(linkLocation.getLastUpdated())
            .withService("4oD");
    }

    static Integer itemIndexFrom(Item item) {
        if (item instanceof Episode) {
            return ((Episode) item).getEpisodeNumber();
        }
        return null;
    }

    static Duration toDuration(Integer seconds) {
        if (seconds != null) {
            return Duration.standardSeconds(seconds);
        }
        return null;
    }
}
