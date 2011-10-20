package org.atlasapi.beans;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.entity.Actor;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.ContentGroup;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.EntityType;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Person;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule.ScheduleChannel;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Topic;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.entity.simple.Aliased;
import org.atlasapi.media.entity.simple.BrandSummary;
import org.atlasapi.media.entity.simple.ContentIdentifier;
import org.atlasapi.media.entity.simple.ContentQueryResult;
import org.atlasapi.media.entity.simple.Description;
import org.atlasapi.media.entity.simple.Item;
import org.atlasapi.media.entity.simple.PeopleQueryResult;
import org.atlasapi.media.entity.simple.PublisherDetails;
import org.atlasapi.media.entity.simple.Restriction;
import org.atlasapi.media.entity.simple.ScheduleQueryResult;
import org.atlasapi.media.entity.simple.SeriesSummary;
import org.atlasapi.media.entity.simple.TopicQueryResult;
import org.atlasapi.persistence.topic.TopicQueryResolver;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.intl.Countries;

/**
 * {@link AtlasModelWriter} that translates the full URIplay object model
 * into a simplified form and renders that as XML.
 *  
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class FullToSimpleModelTranslator implements AtlasModelWriter {

	private final AtlasModelWriter outputWriter;
    private final TopicQueryResolver topicResolver;

	public FullToSimpleModelTranslator(AtlasModelWriter outputter, TopicQueryResolver topicResolver) {
		this.outputWriter = outputter;
        this.topicResolver = topicResolver;
	}
	
	@Override
	public void writeTo(HttpServletRequest request, HttpServletResponse response, Collection<Object> fullGraph, AtlasModelType type) throws IOException {
	    
	    Object outputGraph;
	    if (type.equals(AtlasModelType.PEOPLE)) {
	        outputGraph = writeOutPeople(Iterables.transform(fullGraph, TO_PERSON));
	    }
	    else if (type.equals(AtlasModelType.SCHEDULE)) {
	        outputGraph = writeOutSchedule(Iterables.transform(fullGraph, TO_SCHEDULE_CHANNEL));
	    }
	    else if (type.equals(AtlasModelType.TOPIC)) {
	        outputGraph = writeOutTopics(Iterables.transform(fullGraph, TO_TOPIC));
	    }
	    else {
	        outputGraph = writeOutContent(Iterables.transform(fullGraph, TO_DESCRIBED));
	    }
	    
	    outputWriter.writeTo(request, response, ImmutableSet.of(outputGraph), type);
	}
	
	private TopicQueryResult writeOutTopics(Iterable<Topic> fullTopics) {
	    TopicQueryResult result = new TopicQueryResult();
	    for (Topic fullTopic : fullTopics) {
	        org.atlasapi.media.entity.simple.Topic topic = new org.atlasapi.media.entity.simple.Topic();
	        copyDescriptionAttributesTo(fullTopic, topic);
	        topic.setTitle(fullTopic.getTitle());
	        topic.setDescription(fullTopic.getDescription());
	        topic.setImage(fullTopic.getImage());
	        topic.setImage(fullTopic.getThumbnail());
            topic.setPublishers(ImmutableSet.copyOf(Iterables.transform(fullTopic.getPublishers(), new Function<Publisher, PublisherDetails>() {
                @Override
                public PublisherDetails apply(Publisher input) {
                    return toPublisherDetails(input);
                }
            })));
	        topic.setType(fullTopic.getType().toString());
	        topic.setValue(fullTopic.getValue());
	        topic.setNamespace(fullTopic.getNamespace());
	        result.add(topic);
        }
        return result;
    }

    private PeopleQueryResult writeOutPeople(Iterable<Person> people) {
	    PeopleQueryResult peopleOutputGraph = new PeopleQueryResult();
        for (Person person : people) {
            peopleOutputGraph.add(simplePersonFrom(person));
        }
        return peopleOutputGraph;
    }

    private ScheduleQueryResult writeOutSchedule(Iterable<ScheduleChannel> fullGraph) {
        ScheduleQueryResult outputGraph = new ScheduleQueryResult();
	    for (ScheduleChannel scheduleChannel : fullGraph) {
	        outputGraph.add(scheduleChannelFrom(scheduleChannel));
	    }
	    return outputGraph;
	}

	private ContentQueryResult writeOutContent(Iterable<Described> fullGraph) {
	    ContentQueryResult outputGraph = new ContentQueryResult();
		for (Described described : fullGraph) {
			if (described instanceof Container) {
				Container playList = (Container) described;
				outputGraph.add(simplePlaylistFrom(playList));
			}
			if (described instanceof ContentGroup) {
				ContentGroup group = (ContentGroup) described;
				outputGraph.add(simplePlaylistFrom(group));
			}
			if (described instanceof org.atlasapi.media.entity.Item) {
				outputGraph.add(simpleItemFrom((org.atlasapi.media.entity.Item) described));
			}
		}
		return outputGraph;
	}
	
	org.atlasapi.media.entity.simple.ScheduleChannel scheduleChannelFrom(ScheduleChannel scheduleChannel) {
	    org.atlasapi.media.entity.simple.ScheduleChannel newScheduleChannel = new org.atlasapi.media.entity.simple.ScheduleChannel();
	    newScheduleChannel.setChannelUri(scheduleChannel.channel().uri());
	    newScheduleChannel.setChannelKey(scheduleChannel.channel().key());
	    newScheduleChannel.setChannelTitle(scheduleChannel.channel().title());
	    
	    ImmutableList.Builder<org.atlasapi.media.entity.simple.Item> items = ImmutableList.builder();
	    for (org.atlasapi.media.entity.Item item: scheduleChannel.items()) {
	        items.add(simpleItemFrom(item));
	    }
	    
	    newScheduleChannel.setItems(items.build());
	    return newScheduleChannel;
	}
	
	private static org.atlasapi.media.entity.simple.Person simplePersonFrom(Person fullPerson) {
	    org.atlasapi.media.entity.simple.Person person = new org.atlasapi.media.entity.simple.Person();
	    person.setType(Person.class.getSimpleName());
	    person.setUri(fullPerson.getCanonicalUri());
	    person.setCurie(fullPerson.getCurie());
	    person.setName(fullPerson.getTitle());
	    person.setProfileLinks(fullPerson.getAliases());
	    person.setContent(simpleContentListFrom(fullPerson.getContents()));
	    
	    return person;
	}
	
	private static org.atlasapi.media.entity.simple.Person simplePersonFrom(CrewMember fullCrew) {
	    org.atlasapi.media.entity.simple.Person person = new org.atlasapi.media.entity.simple.Person();
	    person.setType(Person.class.getSimpleName());
	    if (fullCrew instanceof Actor) {
	        Actor fullActor = (Actor) fullCrew;
	        person.setCharacter(fullActor.character());
	    }
	    
	    copyDescriptionAttributesTo(fullCrew, person);
	    person.setName(fullCrew.name());
	    person.setProfileLinks(fullCrew.profileLinks());
	    person.setRole(fullCrew.role().key());
        
	    return person;
	}

	private org.atlasapi.media.entity.simple.Playlist simplePlaylistFrom(Container fullPlayList) {
		
		org.atlasapi.media.entity.simple.Playlist simplePlaylist = new org.atlasapi.media.entity.simple.Playlist();
		simplePlaylist.setType(EntityType.from(fullPlayList).toString());
		    
		copyBasicContentAttributes(fullPlayList, simplePlaylist);
		
		if (fullPlayList instanceof Series) {
		    Series series = (Series) fullPlayList;
            simplePlaylist.setSeriesNumber(series.getSeriesNumber());
		    simplePlaylist.setTotalEpisodes(series.getTotalEpisodes());
		}
		
		for (ChildRef child : fullPlayList.getChildRefs()) {
			simplePlaylist.add(contentIdentifierFrom(child));
		}
		return simplePlaylist;
	}
	
	private static org.atlasapi.media.entity.simple.Playlist simplePlaylistFrom(ContentGroup fullPlayList) {
		
		org.atlasapi.media.entity.simple.Playlist simplePlaylist = new org.atlasapi.media.entity.simple.Playlist();
		
		copyBasicDescribedAttributes(fullPlayList, simplePlaylist);
		
		simplePlaylist.setContent(simpleContentListFrom(fullPlayList.getContents()));
		
		return simplePlaylist;
	}
	
	private static List<ContentIdentifier> simpleContentListFrom(Iterable<ChildRef> contents) {
	    List<ContentIdentifier> contentList = Lists.newArrayList();
	    for (ChildRef ref : contents) {
            contentList.add(ContentIdentifier.identifierFor(ref));
        }
	    return contentList;
	}
	
	private void copyBasicContentAttributes(Content content, Description simpleDescription) {
		copyBasicDescribedAttributes(content, simpleDescription);
		simpleDescription.setClips(clipToSimple(content.getClips()));
		simpleDescription.setTopics(resolveTopics(content.getTopics()));
	}

    private Iterable<org.atlasapi.media.entity.simple.Topic> resolveTopics(List<String> topics) {
        if(topics.isEmpty()) { //don't even ask (the resolver)
            return ImmutableList.of();
        }
        return writeOutTopics(topicResolver.topicsForUris(topics)).getContents();
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
		simpleDescription.setPresentationChannel(content.getPresentationChannel());
		
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

	private List<Item> clipToSimple(List<Clip> clips) {
		return Lists.transform(clips, new Function<Clip, Item>() {
			@Override
			public Item apply(Clip clip) {
				return simpleItemFrom(clip);
			}
		});
	}

    static ContentIdentifier contentIdentifierFrom(ChildRef content) {
        return ContentIdentifier.identifierFor(content);
    }

	org.atlasapi.media.entity.simple.Item simpleItemFrom(org.atlasapi.media.entity.Item fullItem) {
		
		org.atlasapi.media.entity.simple.Item simpleItem = new org.atlasapi.media.entity.simple.Item();
		simpleItem.setType(EntityType.from(fullItem).toString());
		
		for (Version version : fullItem.getVersions()) {
			addTo(simpleItem, version, fullItem);
		}
		
		List<org.atlasapi.media.entity.simple.Person> people = Lists.newArrayList();
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

	private static void addTo(Item simpleItem, Version version, org.atlasapi.media.entity.Item item) {
		
		for (Encoding encoding : version.getManifestedAs()) {
			addTo(simpleItem, version, encoding, item);
		}
		
		for (Broadcast broadcast : version.getBroadcasts()) {
			org.atlasapi.media.entity.simple.Broadcast simpleBroadcast = simplify(broadcast);
			copyProperties(version, simpleBroadcast, item);
			simpleItem.addBroadcast(simpleBroadcast);
		}
	}

	private static org.atlasapi.media.entity.simple.Broadcast simplify(Broadcast broadcast) {
	    org.atlasapi.media.entity.simple.Broadcast simpleModel = new org.atlasapi.media.entity.simple.Broadcast(broadcast.getBroadcastOn(), broadcast.getTransmissionTime(), broadcast.getTransmissionEndTime(), broadcast.getId());
	    
	    simpleModel.setRepeat(broadcast.getRepeat());
	    simpleModel.setSubtitled(broadcast.getSubtitled());
	    simpleModel.setSigned(broadcast.getSigned());
        simpleModel.setAudioDescribed(broadcast.getAudioDescribed());
        simpleModel.setHighDefinition(broadcast.getHighDefinition());
        simpleModel.setWidescreen(broadcast.getWidescreen());
        simpleModel.setSurround(broadcast.getSurround());
        simpleModel.setLive(broadcast.getLive());
        simpleModel.setPremiere(broadcast.getPremiere());
        simpleModel.setNewSeries(broadcast.getNewSeries());
	    
	    return simpleModel; 
	}

	private static void addTo(Item simpleItem, Version version, Encoding encoding, org.atlasapi.media.entity.Item item) {
		for (Location location : encoding.getAvailableAt()) {
			addTo(simpleItem, version, encoding, location, item);
		}
	}

	private static void addTo(Item simpleItem, Version version, Encoding encoding, Location location, org.atlasapi.media.entity.Item item) {
		
		org.atlasapi.media.entity.simple.Location simpleLocation = new org.atlasapi.media.entity.simple.Location();
		
		copyProperties(version, simpleLocation, item);
		copyProperties(encoding, simpleLocation);
		copyProperties(location, simpleLocation);
		
		simpleItem.addLocation(simpleLocation);
	}

	private void copyProperties(org.atlasapi.media.entity.Item fullItem, Item simpleItem) {
		copyBasicContentAttributes(fullItem, simpleItem);
		
		simpleItem.setBlackAndWhite(fullItem.getBlackAndWhite());
		simpleItem.setCountriesOfOrigin(fullItem.getCountriesOfOrigin());
		simpleItem.setScheduleOnly(fullItem.isScheduleOnly());
		
		if (fullItem.getContainer() != null) {
			ParentRef brand = fullItem.getContainer();
			BrandSummary brandSummary = new BrandSummary();
			brandSummary.setUri(brand.getUri());
			simpleItem.setBrandSummary(brandSummary);
		}
		
		if (fullItem instanceof Episode) {
			Episode episode = (Episode) fullItem;
			
			simpleItem.setEpisodeNumber(episode.getEpisodeNumber());
			simpleItem.setSeriesNumber(episode.getSeriesNumber());
			
			ParentRef series = episode.getSeriesRef();
			if (series != null) {
				SeriesSummary seriesSummary = new SeriesSummary();
				seriesSummary.setUri(series.getUri());
				seriesSummary.setType(Series.class.getSimpleName());
				simpleItem.setSeriesSummary(seriesSummary);
			}
		} else if (fullItem instanceof Film) {
            Film film = (Film) fullItem;
		    simpleItem.setYear(film.getYear());
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

	private static void copyProperties(Version version, org.atlasapi.media.entity.simple.Version simpleLocation, org.atlasapi.media.entity.Item item) {

		simpleLocation.setPublishedDuration(version.getPublishedDuration());
		simpleLocation.setDuration(durationFrom(item, version));
		
		Restriction restriction = new Restriction();
		
		if(version.getRestriction() != null) {
			restriction.setRestricted(version.getRestriction().isRestricted());
			restriction.setMinimumAge(version.getRestriction().getMinimumAge());
			restriction.setMessage(version.getRestriction().getMessage());	
		}
		
		simpleLocation.setRestriction(restriction);
	}

	// temporary fix: some versions are missing durations so
	// we fall back to the broadcast and location durations
	private static Integer durationFrom(org.atlasapi.media.entity.Item item, Version version) {
	    if (version.getDuration() != null && version.getDuration() > 0) {
	        return version.getDuration();
	    }
	    Iterable<Broadcast> broadcasts = item.flattenBroadcasts();
	    if (Iterables.isEmpty(broadcasts)) {
	        return null;
	    }
        return Ordering.natural().max(Iterables.transform(broadcasts, new Function<Broadcast, Integer>() {
            @Override
            public Integer apply(Broadcast input) {
               Integer duration = input.getBroadcastDuration();
               if (duration == null) {
                   return 0;
               }
               return duration;
            }
	    }));
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
		if (encoding.getAudioCoding() != null) {
		    simpleLocation.setAudioCoding(encoding.getAudioCoding().toString());
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
	    simpleLocation.setEmbedId(location.getEmbedId());
	    simpleLocation.setAvailable(location.getAvailable());
	    
	}

	@Override
	public void writeError(HttpServletRequest request, HttpServletResponse response, AtlasErrorSummary exception) throws IOException {
		outputWriter.writeError(request, response, exception);
	}
	
	private static final Function<Object, Person> TO_PERSON = new Function<Object, Person>() {
        @Override
        public Person apply(Object input) {
            return (Person) input;
        }
    };
    
    private static final Function<Object, ScheduleChannel> TO_SCHEDULE_CHANNEL = new Function<Object, ScheduleChannel>() {
        @Override
        public ScheduleChannel apply(Object input) {
            return (ScheduleChannel) input;
        }
    };
    
    private static final Function<Object, Topic> TO_TOPIC = new Function<Object, Topic>() {
        @Override
        public Topic apply(Object input) {
            return (Topic) input;
        }
    };
    
    private static final Function<Object, Described> TO_DESCRIBED = new Function<Object, Described>() {
        @Override
        public Described apply(Object input) {
            return (Described) input;
        }
    };
}
