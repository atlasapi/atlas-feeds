package org.atlasapi.feeds.interlinking;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Description;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.metabroadcast.common.text.Truncator;

public class PlaylistToInterlinkFeedAdapter implements PlaylistToInterlinkFeed {
    
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
    
    public InterlinkFeed fromBrands(String id, String publisher, DateTime from, DateTime to, List<Brand> brands) {
        InterlinkFeed feed = feed(id, publisher);
        
        for (Brand brand: brands) {
            InterlinkBrand interlinkBrand = fromBrand(brand);
            if (qualifies(from, to, brand)) {
                feed.addEntry(interlinkBrand);
            }
            
            Map<String, InterlinkSeries> seriesLookup = Maps.newHashMap();
            for (Item item : brand.getItems()) {
                InterlinkSeries linkSeries = null;
                if (item instanceof Episode) {
                    Episode episode = (Episode) item;
                    Series series = episode.getSeriesSummary();
                    if (series != null && qualifies(from, to, series)) {
                        linkSeries = seriesLookup.get(series.getCanonicalUri());
                        if (linkSeries == null) {
                            linkSeries = fromSeries(series, interlinkBrand);
                            feed.addEntry(linkSeries);
                            seriesLookup.put(series.getCanonicalUri(), linkSeries);
                        }
                    }
                }
                
                populateFeedWithItem(feed, item, to, from, (linkSeries != null ? linkSeries : interlinkBrand));
            }
        }
        
        return feed;
    }
    
    static boolean qualifies(DateTime from, DateTime to, Description description) {
        return ((from == null && to == null) || (description != null && description.getLastUpdated() != null && description.getLastUpdated().isAfter(from) && description.getLastUpdated().isBefore(to)));
    }
    
    private InterlinkFeed feed(String id, String publisher) {
        InterlinkFeed feed = new InterlinkFeed(id);

        feed.withAuthor(new InterlinkFeedAuthor(publisher, publisher));
        feed.withUpdatedAt(new DateTime());
        
        return feed;
    }

    private InterlinkSeries fromSeries(Series series, InterlinkBrand brand) {
        return new InterlinkSeries(series.getCanonicalUri(), series.getSeriesNumber(), brand)
        	.withTitle(series.getTitle())
        	.withDescription(series.getDescription())
        	.withLastUpdated(series.getLastUpdated())
        	.withSummary(toSummary(series))
        	.withThumbnail(series.getImage());
    }

    private void populateFeedWithItem(InterlinkFeed feed, Item item, DateTime to, DateTime from, InterlinkContent parent) {
        InterlinkEpisode episode = new InterlinkEpisode(item.getCanonicalUri(), itemIndexFrom(item), item.getCanonicalUri(), parent)
            .withTitle(item.getTitle())
            .withDescription(item.getDescription())
            .withLastUpdated(item.getLastUpdated())
            .withSummary(toSummary(item))
            .withThumbnail(item.getImage());
        
        if (qualifies(from, to, item)) {
            feed.addEntry(episode);
        }

        for (Broadcast broadcast : broadcasts(item)) {
            if (qualifies(from, to, broadcast)) {
                InterlinkBroadcast interlinkBroadcast = fromBroadcast(broadcast, episode);
                if (interlinkBroadcast != null) {
                    feed.addEntry(interlinkBroadcast);
                }
            }
        }

        InterlinkOnDemand onDemand = firstLinkLocation(item, to, from, episode);
        if (onDemand != null) {
            feed.addEntry(onDemand);
        }
    }

    private InterlinkBrand fromBrand(Brand brand) {
        return new InterlinkBrand(brand.getCanonicalUri())
			.withLastUpdated(brand.getLastUpdated())
        	.withTitle(brand.getTitle())
        	.withDescription(brand.getDescription())
        	.withSummary(toSummary(brand))
        	.withThumbnail(brand.getImage());
    }

    private String toSummary(Content content) {
    	String description = content.getDescription();
		if (description == null) {
    		return null;
    	}
    	return summaryTruncator.truncate(description);
    }

    protected InterlinkBroadcast fromBroadcast(Broadcast broadcast, InterlinkEpisode episode) {
        String id = broadcast.getBroadcastOn() + "-" + broadcast.getTransmissionTime().getMillis();
        String service = CHANNEL_LOOKUP.get(broadcast.getBroadcastOn());

        return new InterlinkBroadcast(id, episode)
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

    static InterlinkOnDemand firstLinkLocation(Item item, DateTime to, DateTime from, InterlinkEpisode episode) {
        for (Version version : item.getVersions()) {
            for (Encoding encoding : version.getManifestedAs()) {
                for (Location location : encoding.getAvailableAt()) {
                    if (TransportType.LINK.equals(location.getTransportType()) && qualifies(to, from, location)) {
                        return fromLocation(location, episode, version.getDuration());
                    }
                }
            }
        }
        return null;
    }
    
    static InterlinkOnDemand fromLocation(Location linkLocation, InterlinkEpisode episode, int d) {
        Duration duration = new Duration(d*1000);
        
        return new InterlinkOnDemand(linkLocation.getUri(), linkLocation.getPolicy().getAvailabilityStart(), linkLocation.getPolicy().getAvailabilityEnd(), duration, episode)
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
