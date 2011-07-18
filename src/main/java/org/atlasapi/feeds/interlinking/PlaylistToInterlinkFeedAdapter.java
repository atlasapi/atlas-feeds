package org.atlasapi.feeds.interlinking;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.feeds.interlinking.InterlinkBase.Operation;
import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.metabroadcast.common.text.Truncator;

public class PlaylistToInterlinkFeedAdapter implements PlaylistToInterlinkFeed {
    
    protected static final Operation DEFAULT_OPERATION = Operation.STORE;
	
    public static Map<String, String> CHANNEL_LOOKUP = ImmutableMap.<String, String>builder()
		.put("http://www.channel4.com", "C4")
		.put("http://www.channel4.com/more4", "M4")
		.put("http://www.e4.com", "E4")
		.put("http://film4.com", "F4")
		.put("http://www.4music.com", "4M")
	.build();

	private final Truncator summaryTruncator = new Truncator()
		.withMaxLength(90)
		.onlyTruncateAtAWordBoundary()
		.omitTrailingPunctuationWhenTruncated()
		.onlyStartANewSentenceIfTheSentenceIsAtLeastPercentComplete(50).withOmissionMarker("...");
	
	private final Truncator descriptionTruncator = new Truncator()
        .withMaxLength(180)
        .withOmissionMarker("...")
        .onlyTruncateAtAWordBoundary()
        .omitTrailingPunctuationWhenTruncated()
        .onlyStartANewSentenceIfTheSentenceIsAtLeastPercentComplete(50);
    
    public InterlinkFeed fromContent(String id, Publisher publisher, DateTime from, DateTime to, Iterator<Content> contents) {
        InterlinkFeed feed = feed(id, publisher);
        while(contents.hasNext()) {
            Content content = contents.next();
        	if (content instanceof Brand) {
        		Brand brand = (Brand) content;
        		if (containerQualifies(from, to, brand)) {
        		    feed.addEntry(fromBrand(brand, from, to));
        		}
        	}
        	if (content instanceof Series) {
        	    Series series = (Series) content;
                if (containerQualifies(from, to, series)) {
                    feed.addEntry(fromSeries(series, from, to));
                }
        	}
        	if (content instanceof Item) {
        	    Item item = (Item) content;
        	    populateFeedWithItem(feed, item, from, to);
        	}
        }
        return feed;
    }
    
    static boolean containerQualifies(DateTime from, DateTime to, Container description) {
        return ((from == null && to == null) || (description != null && description.getThisOrChildLastUpdated() != null && description.getThisOrChildLastUpdated().isAfter(from) && description.getThisOrChildLastUpdated().isBefore(to)));
    }
    
    static boolean qualifies(DateTime from, DateTime to, Identified description) {
        return ((from == null && to == null) || (description != null && description.getLastUpdated() != null && description.getLastUpdated().isAfter(from) && description.getLastUpdated().isBefore(to)));
    }
    
    private InterlinkFeed feed(String id, Publisher publisher) {
        InterlinkFeed feed = new InterlinkFeed(id);
        feed.withAuthor(new InterlinkFeedAuthor(toPublisherName(publisher), toPublisherName(publisher)));
        return feed;
    }

	private String toPublisherName(Publisher publisher) {
		return publisher.key().split("\\.")[0];
	}

    private InterlinkSeries fromSeries(Series series, DateTime from, DateTime to) {
        String parentId = series.getParent() == null ? null : idFromParentRef(series.getParent());
        
        return new InterlinkSeries(idFrom(series), operationFor(series, from, to), series.getSeriesNumber(), parentId)
        	.withTitle(extractSeriesTitle(series))
        	.withDescription(toDescription(series))
        	.withLastUpdated(series.getLastUpdated())
        	.withSummary(toSummary(series))
        	.withThumbnail(series.getImage());
    }

	protected String idFromParentRef(ParentRef parent) {
        return parent.getUri();
    }

    private String extractSeriesTitle(Series series) {
		String title = series.getTitle();
		if (! Strings.isNullOrEmpty(title)) {
    		Pattern pattern = Pattern.compile(".*(Series\\s*\\d+).*");
    		Matcher match = pattern.matcher(title);
    		if(match.matches()) {
    			return match.group(1);
    		}
		} else if (series.getSeriesNumber() != null && series.getSeriesNumber() > 0) {
		    return "Series "+series.getSeriesNumber();
		}
		return title;
	}

	private void populateFeedWithItem(InterlinkFeed feed, Item item, DateTime from, DateTime to) {
		String episodeId = idFrom(item);
		String parentId = parentFromItem(item);
		
		InterlinkOnDemand onDemand = firstLinkLocation(item, from, to, episodeId);
		
		InterlinkEpisode episode = new InterlinkEpisode(episodeId, operationFor(item, from, to), itemIndexFrom(item), onDemand == null ? linkFrom(item.getCanonicalUri()) : onDemand.uri(), parentId)
            .withTitle(extractItemTitle(item))
            .withDescription(toDescription(item))
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

        
        if (onDemand != null) {
            feed.addEntry(onDemand);
        }
    }

	private String parentFromItem(Item item) {
	    if (item instanceof Episode) {
	        Episode episode = (Episode) item;
	        if (episode.getSeriesRef() != null) {
	            return idFromParentRef(episode.getSeriesRef());
	        }
	    }
	    if (item.getContainer() != null) {
	        return idFromParentRef(item.getContainer());
	    }
	    return null;
    }

    protected String linkFrom(String canonicalUri) {
	    return canonicalUri;
    }

    private String extractItemTitle(Item item) {
		String title = item.getTitle();
		Pattern pattern = Pattern.compile(".*(Episode\\s*\\d+).*");
		Matcher match = pattern.matcher(title);
		if(match.matches()) {
			return match.group(1);
		}
		return title;
	}

    private InterlinkBrand fromBrand(Brand brand, DateTime from, DateTime to) {
        return new InterlinkBrand(idFrom(brand), operationFor(brand, from, to))
			.withLastUpdated(brand.getLastUpdated())
        	.withTitle(brand.getTitle())
        	.withDescription(toDescription(brand))
        	.withSummary(toSummary(brand))
        	.withThumbnail(brand.getImage());

    }
    
    private Operation operationFor(Series series, DateTime from, DateTime to) {
//    	for (Item item : brand.getContents()) {
//    		if (!(item instanceof Episode)) {
//    			continue;
//    		}
//    		Episode episode = (Episode) item;
//    		Series seriesSummary = episode.getSeries();
//    		if (seriesSummary == null) {
//    			continue;
//    		}
//			if (!seriesSummary.getCanonicalUri().equals(series.getCanonicalUri())) {
//    			continue;
//    		}
//			if (Operation.STORE.equals(operationFor(item, from, to))) {
//				return Operation.STORE;
//			}
//		}
		return Operation.STORE;
    }

	private Operation operationFor(Brand brand, DateTime from, DateTime to) {
		return Operation.STORE;
	}

	private Operation operationFor(Item item, DateTime from, DateTime to) {
		Location location = firstQualifyingLocation(item, from, to);
		
		boolean activeBroadcast = false;
		for (Version version: item.nativeVersions()) {
		    for (Broadcast broadcast: version.getBroadcasts()) {
		        Operation operation = broadcastOperation(broadcast);
		        if (Operation.STORE.equals(operation)) {
		            activeBroadcast = true;
		        }
		    }
		}
		
		if ((location == null || !location.getAvailable()) && (! activeBroadcast)) {
			return Operation.DELETE;
		} else {
			return Operation.STORE;
		}
	}

	protected String idFrom(Identified description) {
		return description.getCanonicalUri();
	}

	private String toSummary(Described content) {
    	String description = content.getDescription();
		if (description == null) {
    		return null;
    	}
    	return summaryTruncator.truncate(description);
    }
	
	private String toDescription(Described content) {
    	String description = content.getDescription();
		if (description == null) {
    		return "";
    	}
    	return descriptionTruncator.truncate(description);
    }

    private InterlinkBroadcast fromBroadcast(Broadcast broadcast, InterlinkEpisode episode) {
        String id = broadcastId(broadcast);
        String service = CHANNEL_LOOKUP.get(broadcast.getBroadcastOn());
        
        Operation operation = broadcastOperation(broadcast);

        return new InterlinkBroadcast(id, operation, episode)
    		.withLastUpdated(broadcast.getLastUpdated())
        	.withDuration(toDuration(broadcast.getBroadcastDuration()))
        	.withBroadcastStart(broadcast.getTransmissionTime())
        	.withService(service);
    }

	protected String broadcastId(Broadcast broadcast) {
		return broadcast.getBroadcastOn() + "-" + broadcast.getTransmissionTime().getMillis();
	}
    
    private Operation broadcastOperation(Broadcast broadcast) {
        Operation operation = Operation.STORE;
        if (Boolean.FALSE.equals(broadcast.isActivelyPublished())) {
            operation = Operation.DELETE;
        }
        return operation;
    }

    private Set<Broadcast> broadcasts(Item item) {
    	Predicate<Broadcast> predicate = broadcastFilter();
        Set<Broadcast> broadcasts = Sets.newHashSet();
        for (Version version : item.nativeVersions()) {
            for (Broadcast broadcast : version.getBroadcasts()) {
            	if (predicate.apply(broadcast)) {
            		broadcasts.add(broadcast);
            	}
            }
        }
        return broadcasts;
    }
    
    protected Predicate<Broadcast> broadcastFilter() {
    	return Predicates.alwaysTrue();
    }
    
    private Location firstQualifyingLocation(Item item, DateTime from, DateTime to) {
	   for (Version version : item.nativeVersions()) {
           for (Encoding encoding : version.getManifestedAs()) {
               for (Location location : encoding.getAvailableAt()) {
                   if (TransportType.LINK.equals(location.getTransportType()) && qualifies(from, to, location)) {
                	   return location;
                   }
               }
           }
	   }
	   return null;
    }
    
    protected InterlinkOnDemand firstLinkLocation(Item item, DateTime from, DateTime to, String parentId) {
        for (Version version : item.nativeVersions()) {
            for (Encoding encoding : version.getManifestedAs()) {
                for (Location location : encoding.getAvailableAt()) {
                    if (TransportType.LINK.equals(location.getTransportType()) && qualifies(from, to, location)) {
                        return fromLocation(location, parentId, version.getDuration());
                    }
                }
            }
        }
        return null;
    }
    
    protected InterlinkOnDemand fromLocation(Location linkLocation, String parentId, Integer durationInSeconds) {
        Duration duration = durationInSeconds == null ? null : Duration.standardSeconds(durationInSeconds);
        Operation operation = linkLocation.getAvailable() ? Operation.STORE : Operation.DELETE;
        
        return new InterlinkOnDemand(idFrom(linkLocation), linkLocation.getUri(), operation, linkLocation.getPolicy().getAvailabilityStart(), linkLocation.getPolicy().getAvailabilityEnd(), duration, parentId)
            .withLastUpdated(linkLocation.getLastUpdated())
            .withService("4od");
    }

    protected Integer itemIndexFrom(Item item) {
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
