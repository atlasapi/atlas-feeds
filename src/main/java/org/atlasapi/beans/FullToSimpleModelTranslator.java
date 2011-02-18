package org.atlasapi.beans;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.entity.Actor;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Channel;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.ContentGroup;
import org.atlasapi.media.entity.Countries;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.entity.Schedule.ScheduleChannel;
import org.atlasapi.media.entity.simple.Aliased;
import org.atlasapi.media.entity.simple.BrandSummary;
import org.atlasapi.media.entity.simple.ContentQueryResult;
import org.atlasapi.media.entity.simple.Description;
import org.atlasapi.media.entity.simple.Item;
import org.atlasapi.media.entity.simple.PublisherDetails;
import org.atlasapi.media.entity.simple.Restriction;
import org.atlasapi.media.entity.simple.ScheduleQueryResult;
import org.atlasapi.media.entity.simple.SeriesSummary;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.metabroadcast.common.base.Maybe;

/**
 * {@link AtlasModelWriter} that translates the full URIplay object model
 * into a simplified form and renders that as XML.
 *  
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class FullToSimpleModelTranslator implements AtlasModelWriter {

	private final AtlasModelWriter outputWriter;

	public FullToSimpleModelTranslator(AtlasModelWriter outputter) {
		this.outputWriter = outputter;
	}
	
	@Override
	public void writeTo(HttpServletRequest request, HttpServletResponse response, Collection<Object> fullGraph) throws IOException {
		ContentQueryResult outputGraph = new ContentQueryResult();
		writeOutContent(fullGraph, outputGraph);
		
		if (outputGraph.isEmpty()) {
		    ScheduleQueryResult scheduleQueryResult = new ScheduleQueryResult();
		    writeOutSchedule(fullGraph, scheduleQueryResult);
		    
		    if (! scheduleQueryResult.isEmpty()) {
		        outputWriter.writeTo(request, response, ImmutableSet.of((Object) scheduleQueryResult));
		        return;
		    }
		}
		
		outputWriter.writeTo(request, response, ImmutableSet.of((Object) outputGraph));
	}
	
	private void writeOutSchedule(Collection<Object> fullGraph, ScheduleQueryResult outputGraph) {
	    for (Object bean : fullGraph) {
	        if (bean instanceof ScheduleChannel) {
	            outputGraph.add(scheduleChannelFrom((ScheduleChannel) bean));
	        }
	    }
	}

	private void writeOutContent(Collection<Object> fullGraph, ContentQueryResult outputGraph) {
		for (Object bean : fullGraph) {
			if (bean instanceof Container<?>) {
				Container<?> playList = (Container<?>) bean;
				outputGraph.add(simplePlaylistFrom(playList));
			}
			if (bean instanceof ContentGroup) {
				ContentGroup group = (ContentGroup) bean;
				outputGraph.add(simplePlaylistFrom(group));
			}
			if (bean instanceof org.atlasapi.media.entity.Item) {
				outputGraph.add(simpleItemFrom((org.atlasapi.media.entity.Item) bean));
			}
		}
	}
	
	static org.atlasapi.media.entity.simple.ScheduleChannel scheduleChannelFrom(ScheduleChannel scheduleChannel) {
	    org.atlasapi.media.entity.simple.ScheduleChannel newScheduleChannel = new org.atlasapi.media.entity.simple.ScheduleChannel();
	    Maybe<Channel> channel = Channel.fromUri(scheduleChannel.channel());
	    newScheduleChannel.setChannelUri(scheduleChannel.channel());
	    if (! channel.isNothing()) {
	        newScheduleChannel.setChannelKey(channel.requireValue().key());
	        newScheduleChannel.setChannelTitle(channel.requireValue().title());
	    }
	    
	    ImmutableList.Builder<org.atlasapi.media.entity.simple.Item> items = ImmutableList.builder();
	    for (org.atlasapi.media.entity.Item item: scheduleChannel.items()) {
	        items.add(simpleItemFrom(item));
	    }
	    
	    newScheduleChannel.setItems(items.build());
	    return newScheduleChannel;
	}
	
	private static org.atlasapi.media.entity.simple.Person simplePersonFrom(CrewMember fullCrew) {
	    org.atlasapi.media.entity.simple.Person person = null;
	    
	    if (fullCrew instanceof Actor) {
	        Actor fullActor = (Actor) fullCrew;
	        person = new org.atlasapi.media.entity.simple.Actor().withCharacter(fullActor.character());
	    } else {
	        person = new org.atlasapi.media.entity.simple.CrewMember();
	    }
	    
	    copyDescriptionAttributesTo(fullCrew, person);
	    person.setName(fullCrew.name());
	    person.setProfileLinks(fullCrew.profileLinks());
	    person.setRole(fullCrew.role().key());
        
	    return person;
	}

	private static org.atlasapi.media.entity.simple.Playlist simplePlaylistFrom(Container<?> fullPlayList) {
		
		org.atlasapi.media.entity.simple.Playlist simplePlaylist = new org.atlasapi.media.entity.simple.Playlist();
		
		copyBasicContentAttributes(fullPlayList, simplePlaylist);
		
		for (org.atlasapi.media.entity.Item fullItem : fullPlayList.getContents()) {
			simplePlaylist.add(simpleItemFrom(fullItem));
		}
		return simplePlaylist;
	}
	
	private static org.atlasapi.media.entity.simple.Playlist simplePlaylistFrom(ContentGroup fullPlayList) {
		
		org.atlasapi.media.entity.simple.Playlist simplePlaylist = new org.atlasapi.media.entity.simple.Playlist();
		
		copyBasicDescribedAttributes(fullPlayList, simplePlaylist);
		
		for (Content fullContent : fullPlayList.getContents()) {
			if (fullContent instanceof org.atlasapi.media.entity.Item) {
				simplePlaylist.add(simpleItemFrom((org.atlasapi.media.entity.Item) fullContent));
			} else if (fullContent instanceof org.atlasapi.media.entity.Container<?>) {
				simplePlaylist.add(simplePlaylistFrom((org.atlasapi.media.entity.Container<?>) fullContent));
			} else {
				throw new IllegalArgumentException("Cannot convert Content of type " + fullContent.getClass().getSimpleName() + " to a simple format");
			}
		}
		return simplePlaylist;
	}
	
	private static void copyBasicContentAttributes(Content content, Description simpleDescription) {
		copyBasicDescribedAttributes(content, simpleDescription);
		simpleDescription.setClips(clipToSimple(content.getClips()));
	}

	private static void copyBasicDescribedAttributes(Described content, Description simpleDescription) {
		copyDescriptionAttributesTo(content, simpleDescription);
		simpleDescription.setTitle(content.getTitle());
		simpleDescription.setPublisher(toPublisherDetails(content.getPublisher()));
		simpleDescription.setDescription(content.getDescription());
		simpleDescription.setImage(content.getImage());
		simpleDescription.setThumbnail(content.getThumbnail());
		simpleDescription.setGenres(content.getGenres());
		simpleDescription.setTags(content.getTags());
		simpleDescription.setSameAs(content.getEquivalentTo());
		
		MediaType mediaType = content.getMediaType();
		if (mediaType != null) {
			simpleDescription.setMediaType(mediaType.toString().toLowerCase());
		}
		if (content.getSpecialization() != null) {
		    simpleDescription.setSpecialization(content.getSpecialization().toString().toLowerCase());
		}
	}

	private static void copyDescriptionAttributesTo(org.atlasapi.media.entity.Identified description, Aliased simpleDescription) {
		simpleDescription.setUri(description.getCanonicalUri());
		simpleDescription.setAliases(description.getAliases());
		simpleDescription.setCurie(description.getCurie());
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
		
		Set<org.atlasapi.media.entity.simple.Person> people = Sets.newHashSet();
		for (CrewMember crew : fullItem.people()) {
		    org.atlasapi.media.entity.simple.Person simplePerson = simplePersonFrom(crew);
		    if (simplePerson != null) {
		        people.add(simplePerson);
		    }
		}
		simpleItem.setPeople(people);
		
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
		return new org.atlasapi.media.entity.simple.Broadcast(broadcast.getBroadcastOn(), broadcast.getTransmissionTime(), broadcast.getTransmissionEndTime(), broadcast.getId());
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
			
			if (episode.getContainer() != null) {
				Container<?> brand = episode.getContainer();
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
		
		Restriction restriction = new Restriction();
		
		if(version.getRestriction() != null) {
			restriction.setRestricted(version.getRestriction().isRestricted());
			restriction.setMinimumAge(version.getRestriction().getMinimumAge());
			restriction.setMessage(version.getRestriction().getMessage());	
		}
		
		simpleLocation.setRestriction(restriction);
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
	public void writeError(HttpServletRequest request, HttpServletResponse response, AtlasErrorSummary exception) throws IOException {
		outputWriter.writeError(request, response, exception);
	}
}
