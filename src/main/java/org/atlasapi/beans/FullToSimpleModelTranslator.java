package org.atlasapi.beans;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Countries;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.entity.simple.BrandSummary;
import org.atlasapi.media.entity.simple.ContentQueryResult;
import org.atlasapi.media.entity.simple.Description;
import org.atlasapi.media.entity.simple.Item;
import org.atlasapi.media.entity.simple.PublisherDetails;
import org.atlasapi.media.entity.simple.SeriesSummary;
import org.atlasapi.media.util.ChildFinder;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * {@link BeanGraphWriter} that translates the full URIplay object model
 * into a simplified form and renders that as XML.
 *  
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class FullToSimpleModelTranslator implements BeanGraphWriter {

	private final BeanGraphWriter outputWriter;

	public FullToSimpleModelTranslator(BeanGraphWriter xmlOutputter) {
		this.outputWriter = xmlOutputter;
	}

	private Iterable<Object> rootsOf(Collection<Object> beans) {
		return Iterables.filter(beans, Predicates.not(new ChildFinder(beans)));
	}
	
	@Override
	public void writeTo(Collection<Object> fullGraph, OutputStream stream) {
		
		ContentQueryResult outputGraph = new ContentQueryResult();
		Set<Object> processed = Sets.newHashSet();
		
		Iterable<Object> beansToProcess = rootsOf(fullGraph);
		
		for (Object bean : beansToProcess) {
			
			if (bean instanceof org.atlasapi.media.entity.Playlist && !processed.contains(bean)) {
				
				org.atlasapi.media.entity.Playlist playList = (org.atlasapi.media.entity.Playlist) bean;
				outputGraph.addPlaylist(simplePlaylistFrom(playList, processed));
				processed.add(playList);
			}
			
		}
	
		for (Object bean : fullGraph) {
			
			if (bean instanceof org.atlasapi.media.entity.Item && !processed.contains(bean)) {
				
				outputGraph.addItem(simpleItemFrom((org.atlasapi.media.entity.Item) bean));
				
			}
			
		}
		
		outputWriter.writeTo(Sets.newHashSet((Object) outputGraph), stream);
	}

	static org.atlasapi.media.entity.simple.Playlist simplePlaylistFrom(Playlist fullPlayList, Set<Object> processed) {

		org.atlasapi.media.entity.simple.Playlist simplePlaylist = new org.atlasapi.media.entity.simple.Playlist();
		
		copyBasicContentAttributes(fullPlayList, simplePlaylist);
		
		for (Playlist fullSubList : fullPlayList.getPlaylists()) {
			simplePlaylist.addPlaylist(simplePlaylistFrom(fullSubList, processed));
			processed.add(fullSubList);
		}
		
		for (org.atlasapi.media.entity.Item fullItem : fullPlayList.getItems()) {
			simplePlaylist.addItem(simpleItemFrom(fullItem));
			processed.add(fullItem);
		}
		
		return simplePlaylist;
	}
	
	private static void copyBasicContentAttributes(Content content, Description simpleDescription) {
		simpleDescription.setUri(content.getCanonicalUri());
		simpleDescription.setAliases(content.getAliases());
		simpleDescription.setCurie(content.getCurie());
		simpleDescription.setTitle(content.getTitle());
		simpleDescription.setPublisher(toPublisherDetails(content.getPublisher()));
		simpleDescription.setDescription(content.getDescription());
		simpleDescription.setImage(content.getImage());
		simpleDescription.setThumbnail(content.getThumbnail());
		simpleDescription.setContainedIn(content.getContainedInUris());
		simpleDescription.setGenres(content.getGenres());
		simpleDescription.setTags(content.getTags());
		simpleDescription.setClips(clipToSimple(content.getClips()));
		simpleDescription.setSameAs(content.getEquivalentTo());
	}

	private static List<Item> clipToSimple(List<Clip> clips) {
		return Lists.transform(clips, new Function<Clip, Item>() {
			@Override
			public Item apply(Clip clip) {
				return simpleItemFrom(clip);
			}
		});
	}

	static org.atlasapi.media.entity.simple.Item simpleItemFrom(org.atlasapi.media.entity.Item fullItem) {
		
		org.atlasapi.media.entity.simple.Item simpleItem = new org.atlasapi.media.entity.simple.Item();
		
		for (Version version : fullItem.getVersions()) {
			addTo(simpleItem, version);
		}
		
		copyProperties(fullItem, simpleItem);
		
		return simpleItem;
	}

	private static void addTo(Item simpleItem, Version version) {
		
		for (Encoding encoding : version.getManifestedAs()) {
			addTo(simpleItem, version, encoding);
		}
		
		for (Broadcast broadcast : version.getBroadcasts()) {
			org.atlasapi.media.entity.simple.Broadcast simpleBroadcast = simplify(broadcast);
			copyProperties(version, simpleBroadcast);
			simpleItem.addBroadcast(simpleBroadcast);
		}
	}

	private static org.atlasapi.media.entity.simple.Broadcast simplify(Broadcast broadcast) {
		return new org.atlasapi.media.entity.simple.Broadcast(broadcast.getBroadcastOn(), broadcast.getTransmissionTime(), broadcast.getTransmissionEndTime());
	}

	private static void addTo(Item simpleItem, Version version, Encoding encoding) {
		for (Location location : encoding.getAvailableAt()) {
			addTo(simpleItem, version, encoding, location);
		}
	}

	private static void addTo(Item simpleItem, Version version, Encoding encoding, Location location) {
		
		org.atlasapi.media.entity.simple.Location simpleLocation = new org.atlasapi.media.entity.simple.Location();
		
		copyProperties(version, simpleLocation);
		copyProperties(encoding, simpleLocation);
		copyProperties(location, simpleLocation);
		
		simpleItem.addLocation(simpleLocation);
	}

	private static void copyProperties(org.atlasapi.media.entity.Item fullItem, Item simpleItem) {
		copyBasicContentAttributes(fullItem, simpleItem);
		
		if (fullItem instanceof Episode) {
			Episode episode = (Episode) fullItem;
			
			simpleItem.setEpisodeNumber(episode.getEpisodeNumber());
			simpleItem.setSeriesNumber(episode.getSeriesNumber());
			
			if (episode.getBrand() != null) {
				Brand brand = episode.getBrand();
				BrandSummary brandSummary = new BrandSummary();

				brandSummary.setUri(brand.getCanonicalUri());
				brandSummary.setCurie(brand.getCurie());
				brandSummary.setTitle(brand.getTitle());
				brandSummary.setDescription(brand.getDescription());
				
				simpleItem.setBrandSummary(brandSummary);
			}
			
			Series series = episode.getSeriesSummary();
			if (series != null) {
				
				SeriesSummary seriesSummary = new SeriesSummary();

				seriesSummary.setUri(series.getCanonicalUri());
				seriesSummary.setCurie(series.getCurie());
				seriesSummary.setTitle(series.getTitle());
				seriesSummary.setDescription(series.getDescription());
				
				simpleItem.setSeriesSummary(seriesSummary);
			}
			
		}
		
	
	}

	private static PublisherDetails toPublisherDetails(Publisher publisher) {

		if (publisher == null) {
			return null;
		}
		
		PublisherDetails details = new PublisherDetails(publisher.key());
		
		if (publisher.country() != null) {
			details.setCountry(publisher.country().code());
		}
		
		details.setName(publisher.title());
		return details;
	}

	private static void copyProperties(Version version, org.atlasapi.media.entity.simple.Version simpleLocation) {

		simpleLocation.setPublishedDuration(version.getPublishedDuration());
		simpleLocation.setDuration(version.getDuration());
		simpleLocation.setRating(version.getRating());
		simpleLocation.setRatingText(version.getRatingText());
	}

	private static void copyProperties(Encoding encoding, org.atlasapi.media.entity.simple.Location simpleLocation) {

		simpleLocation.setAdvertisingDuration(encoding.getAdvertisingDuration());
		simpleLocation.setAudioBitRate(encoding.getAudioBitRate());
		simpleLocation.setAudioChannels(encoding.getAudioChannels());
		simpleLocation.setBitRate(encoding.getBitRate());
		simpleLocation.setContainsAdvertising(encoding.getContainsAdvertising());
		if (encoding.getDataContainerFormat() != null) {
			simpleLocation.setDataContainerFormat(encoding.getDataContainerFormat().toString());
		}
		simpleLocation.setDataSize(encoding.getDataSize());
		simpleLocation.setDistributor(encoding.getDistributor());
		simpleLocation.setHasDOG(encoding.getHasDOG());
		simpleLocation.setSource(encoding.getSource());
		simpleLocation.setVideoAspectRatio(encoding.getVideoAspectRatio());
		simpleLocation.setVideoBitRate(encoding.getVideoBitRate());
		
		if (encoding.getVideoCoding() != null) {
			simpleLocation.setVideoCoding(encoding.getVideoCoding().toString());
		}
		
		simpleLocation.setVideoFrameRate(encoding.getVideoFrameRate());
		simpleLocation.setVideoHorizontalSize(encoding.getVideoHorizontalSize());
		simpleLocation.setVideoProgressiveScan(encoding.getVideoProgressiveScan());
		simpleLocation.setVideoVerticalSize(encoding.getVideoVerticalSize());
	}

	private static void copyProperties(Location location, org.atlasapi.media.entity.simple.Location simpleLocation) {
		Policy policy = location.getPolicy();
		if (policy != null) {
			if (policy.getAvailabilityStart() != null) {
				simpleLocation.setAvailabilityStart(policy.getAvailabilityStart().toDate());
			}
			if (policy.getAvailabilityEnd() != null) {
				simpleLocation.setAvailabilityEnd(policy.getAvailabilityEnd().toDate());
			}
			if (policy.getDrmPlayableFrom() != null) {
				simpleLocation.setDrmPlayableFrom(policy.getDrmPlayableFrom().toDate());
			}
			if (policy.getAvailableCountries() != null) {
				simpleLocation.setAvailableCountries(Countries.toCodes(policy.getAvailableCountries()));
			}
			if (policy.getRevenueContract() != null) {
			    simpleLocation.setRevenueContract(policy.getRevenueContract().key());
			}
			if (policy.getPrice() != null) {
			    simpleLocation.setPrice(policy.getPrice().getAmount());
			    simpleLocation.setCurrency(policy.getPrice().getCurrency().getCurrencyCode());
			}
		}
		
		simpleLocation.setTransportIsLive(location.getTransportIsLive());
	    if (location.getTransportType() != null) {
	    	simpleLocation.setTransportType(location.getTransportType().toString());
	    }
	    if (location.getTransportSubType() != null) {
	    	simpleLocation.setTransportSubType(location.getTransportSubType().toString());
	    }
	    simpleLocation.setUri(location.getUri());
	    simpleLocation.setEmbedCode(location.getEmbedCode());
	    simpleLocation.setAvailable(location.getAvailable());
	    
	}

	@Override
	public void writeError(AtlasErrorSummary exception, OutputStream oStream) {
		outputWriter.writeError(exception, oStream);
	}
}
