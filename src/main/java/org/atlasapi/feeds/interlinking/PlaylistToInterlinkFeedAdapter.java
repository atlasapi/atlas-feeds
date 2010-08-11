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

public class PlaylistToInterlinkFeedAdapter {
    
    private static final String BROADCAST_ID_PREFIX = "tag:";

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
        return new InterlinkSeries(series.getCanonicalUri(), series.getSeriesNumber()).withTitle(series.getTitle()).withSummary(series.getDescription());
    }

    private InterlinkEpisode fromItem(Item item) {
        InterlinkEpisode episode = new InterlinkEpisode(item.getCanonicalUri(), itemIndexFrom(item)).withTitle(item.getTitle());

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
        return new InterlinkBrand(brand.getCanonicalUri()).withTitle(brand.getTitle()).withSummary(brand.getDescription());
    }

    private InterlinkOnDemand fromLocation(Location linkLocation) {
        return new InterlinkOnDemand(linkLocation.getUri());
    }

    private InterlinkBroadcast fromBroadcast(Broadcast broadcast) {
        String id = null;
        for (String alias : broadcast.getAliases()) {
            if (alias.startsWith(BROADCAST_ID_PREFIX) || alias.startsWith("urn:"+BROADCAST_ID_PREFIX)) {
                id = alias;
                break;
            }
        }

        if (id != null) {
            return new InterlinkBroadcast(id).withDuration(toDuration(broadcast.getBroadcastDuration())).withBroadcastStart(broadcast.getTransmissionTime());
        }
        return null;
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
