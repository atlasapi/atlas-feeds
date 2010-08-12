package org.atlasapi.feeds.interlinking;

import java.util.Map;
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
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.metabroadcast.common.time.DateTimeZones;

public class PlaylistToInterlinkFeedAdapter implements PlaylistToInterlinkFeed {

    public InterlinkFeed fromPlaylist(Playlist playlist) {
        InterlinkFeed feed = new InterlinkFeed(playlist.getCanonicalUri());

        feed.withAuthor(new InterlinkFeedAuthor(playlist.getPublisher().name(), playlist.getPublisher().name()));
        DateTime lastUpdated = null;

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
                    
                    if (brand.lastUpdated() != null && (lastUpdated == null || brand.lastUpdated().isAfter(lastUpdated))) {
                        lastUpdated = brand.lastUpdated();
                    }
                }
            }
        }
        
        feed.withUpdatedAt(lastUpdated != null ? lastUpdated : new DateTime(DateTimeZones.UTC));
        
        return feed;
    }

    private InterlinkSeries fromSeries(Series series) {
        return new InterlinkSeries(series.getCanonicalUri(), series.getSeriesNumber()).withTitle(series.getTitle()).withSummary(series.getDescription()).withLastUpdated(series.getLastUpdated());
    }

    private InterlinkEpisode fromItem(Item item) {
        InterlinkEpisode episode = new InterlinkEpisode(item.getCanonicalUri(), itemIndexFrom(item), item.getCanonicalUri()).withTitle(item.getTitle()).withLastUpdated(item.getLastUpdated());

        for (Broadcast broadcast : broadcasts(item)) {
            InterlinkBroadcast interlinkBroadcast = fromBroadcast(broadcast);
            if (interlinkBroadcast != null) {
                episode.addBroadcast(interlinkBroadcast);
            }
        }

        Location linkLocation = firstLinkLocation(item);

        if (linkLocation != null) {
            episode.addOnDemand(fromLocation(linkLocation));
        }

        return episode;
    }

    private InterlinkBrand fromBrand(Brand brand) {
        return new InterlinkBrand(brand.getCanonicalUri()).withTitle(brand.getTitle()).withSummary(brand.getDescription()).withLastUpdated(brand.getLastUpdated());
    }

    private InterlinkOnDemand fromLocation(Location linkLocation) {
        return new InterlinkOnDemand(linkLocation.getUri()).withLastUpdated(linkLocation.getLastUpdated());
    }

    protected InterlinkBroadcast fromBroadcast(Broadcast broadcast) {
        String id = broadcast.getBroadcastOn() + "-" + broadcast.getTransmissionTime().getMillis();

        return new InterlinkBroadcast(id).withDuration(toDuration(broadcast.getBroadcastDuration())).withBroadcastStart(broadcast.getTransmissionTime()).withLastUpdated(broadcast.getLastUpdated());
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

    static Location firstLinkLocation(Item item) {
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
