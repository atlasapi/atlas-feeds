package org.atlasapi.feeds.radioplayer.outputting;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Element;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerPiFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Network;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.format.ISOPeriodFormat;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Lists;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerProgrammeInformationOutputter extends RadioPlayerXMLOutputter {

    private static final String ORIGINATOR = "Metabroadcast";
    private static final String ONDEMAND_LOCATION = "http://www.bbc.co.uk/radio/player/";
    private static final DateTime MAX_AVAILABLE_TILL = new DateTime(2037, 01, 01, 0, 0, 0, 0, DateTimeZones.UTC);

    private final RadioPlayerGenreElementCreator genreElementCreator = new RadioPlayerGenreElementCreator();

    @Override
    public Element createFeed(RadioPlayerFeedSpec spec, Iterable<RadioPlayerBroadcastItem> items) {
        Preconditions.checkArgument(spec instanceof RadioPlayerPiFeedSpec);
        
        RadioPlayerPiFeedSpec piSpec = (RadioPlayerPiFeedSpec) spec;
        
        Element epgElem = createElement("epg", EPGSCHEDULE);
        EPGDATATYPES.addDeclarationTo(epgElem);
        XSI.addDeclarationTo(epgElem);
        RADIOPLAYER.addDeclarationTo(epgElem);
        epgElem.addAttribute(new Attribute("xsi:schemaLocation", XSI.getUri(), SCHEMALOCATION));
        epgElem.addAttribute(new Attribute("system", "DAB"));
        epgElem.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        Element schedule = createElement("schedule", EPGSCHEDULE);
        schedule.addAttribute(new Attribute("originator", ORIGINATOR));
        schedule.addAttribute(new Attribute("version", "1"));
        schedule.addAttribute(new Attribute("creationTime", DATE_TIME_FORMAT.print(new DateTime(DateTimeZones.UTC))));

        schedule.appendChild(scopeElement(piSpec.getDay(), piSpec.getService()));

        for (RadioPlayerBroadcastItem item : items) {
            schedule.appendChild(createProgrammeElement(item, piSpec.getService()));
        }

        epgElem.appendChild(schedule);
        return epgElem;
    }

    private Element createProgrammeElement(RadioPlayerBroadcastItem broadcastItem, RadioPlayerService id) {
        Element programme = createElement("programme", EPGSCHEDULE);
        programme.addAttribute(new Attribute("shortId", "0"));
        programme.addAttribute(new Attribute("id", createCridFromUri(broadcastItem.getItem().getCanonicalUri())));

        String title = itemTitle(broadcastItem);
        programme.appendChild(stringElement("mediumName", EPGDATATYPES, MEDIUM_TITLE.truncatePossibleNull(title)));
        programme.appendChild(stringElement("longName", EPGDATATYPES, LONG_TITLE.truncatePossibleNull(title)));

        Broadcast broadcast = broadcastItem.getBroadcast();
        programme.appendChild(locationElement(broadcast, id));
        programme.appendChild(mediaDescription(stringElement("shortDescription", EPGDATATYPES, SHORT_DESC.truncatePossibleNull(broadcastItem.getItem().getDescription()))));
        if (!Strings.isNullOrEmpty(broadcastItem.getItem().getImage())) {
            programme.appendChild(mediaDescription(imageDescriptionElem(broadcastItem.getItem())));
        }

        for (Element genreElement : genreElementCreator.genreElementsFor(broadcastItem.getItem())) {
            programme.appendChild(genreElement);
        }

        //Because outputCountries always contains ALL, international block output is suppressed.
        Multimap<Country, Location> locationsByCountry = ArrayListMultimap.create();
        // bucket locations by country
        for (Encoding encoding : broadcastItem.getVersion().getManifestedAs()) {
            for (Location location : encoding.getAvailableAt()) {
                for (Country country : representedBy(encoding, location)) {
                    locationsByCountry.put(country, location);
                }
            }
        }
        
        for (Country country : locationsByCountry.keySet()) {
            if (!country.equals(Countries.ALL)) {
                programme.appendChild(ondemandElement(broadcastItem, locationsByCountry.get(country), id));
            }
        }
        
        return programme;
    }
    
    private final Set<Country> representedBy(Encoding encoding, Location location) {
    	Policy policy = location.getPolicy();
		if (policy == null) {
    		return ImmutableSet.of();
    	}
		Set<Country> countries = Sets.newHashSet();
		if (policy.getAvailableCountries().contains(Countries.ALL)) {
			countries.add(Countries.ALL);
			countries.add(Countries.GB);
		}
		if (policy.getAvailableCountries().contains(Countries.GB)) {
			countries.add(Countries.GB);
		}
		if (policy.getAvailableCountries().isEmpty()) {
			countries.add(Countries.ALL);
		}
		return countries;
    }

    private String itemTitle(RadioPlayerBroadcastItem broadcastItem) {
        String title = Strings.nullToEmpty(broadcastItem.getItem().getTitle());
        if (broadcastItem.hasContainer()) {
            Container brand = broadcastItem.getContainer();
            if (!Strings.isNullOrEmpty(brand.getTitle())) {
                String brandTitle = brand.getTitle();
                if (!brandTitle.equals(title)) {
                    return brandTitle + " : " + title;
                }
            }
        }
        return title;
    }

    private Element locationElement(Broadcast broadcast, RadioPlayerService id) {
        Element locationElement = createElement("location", EPGDATATYPES);

        Element timeElement = createElement("time", EPGDATATYPES);
        timeElement.addAttribute(new Attribute("time", DATE_TIME_FORMAT.print(broadcast.getTransmissionTime())));

        Duration duration = new Duration(broadcast.getBroadcastDuration().intValue() * 1000);
        timeElement.addAttribute(new Attribute("duration", ISOPeriodFormat.standard().print(duration.toPeriod())));

        Element bearerElement = createElement("bearer", EPGDATATYPES);
        bearerElement.addAttribute(new Attribute("radioplayerId", id.getRadioplayerId() + ""));
        bearerElement.addAttribute(new Attribute("id", id.getDabServiceId().replace("_", ".")));

        locationElement.appendChild(timeElement);
        locationElement.appendChild(bearerElement);
        return locationElement;
    }

    private Element mediaDescription(Element childElem) {
        Element descriptionElement = createElement("mediaDescription", EPGDATATYPES);
        descriptionElement.appendChild(childElem);
        return descriptionElement;
    }

    private Element imageDescriptionElem(Item item) {
        Element imageElement = createElement("multimedia", EPGDATATYPES);
        imageElement.addAttribute(new Attribute("mimeValue", "image/jpeg"));
        imageElement.addAttribute(new Attribute("url", imageLocationFrom(item)));
        imageElement.addAttribute(new Attribute("width", "86"));
        imageElement.addAttribute(new Attribute("height", "48"));
        return imageElement;
    }

    private String imageLocationFrom(Item item) {
        Pattern p = Pattern.compile("(.*)_\\d+_\\d+.jpg");
        Matcher m = p.matcher(item.getImage());
        if (m.matches()) {
            return m.group(1) + "_86_48.jpg";
        }
        return item.getImage();
    }

    Element ondemandElement(RadioPlayerBroadcastItem broadcastItem,  Collection<Location> locations, RadioPlayerService service) {
        
        Item item = broadcastItem.getItem();
        
        Element ondemandElement = createElement("ondemand", EPGDATATYPES);

        ondemandElement.appendChild(stringElement("player", RADIOPLAYER, ONDEMAND_LOCATION + item.getCurie().substring(item.getCurie().indexOf(":") + 1)));


        Version version = broadcastItem.getVersion();
        
        // get the list of non-null policies from the provided list of locations
        List<Policy> policies = Lists.newArrayList(Iterables.filter(Iterables.transform(locations, new Function<Location, Policy>() {
            @Override
            public Policy apply(Location input) {
                return input.getPolicy();
            }
        }), new Predicate<Policy>() {
            @Override
            public boolean apply(Policy input) {
                return input != null;
            }
        }));
        
        if (policies.isEmpty()) {
            addAudioStreamElement(ondemandElement, version, service);
        } else {
            Optional<Policy> pcPolicy = Iterables.tryFind(policies, new Predicate<Policy>() {
                @Override
                public boolean apply(Policy input) {
                    return (input.getPlatform() != null && input.getPlatform().equals(Platform.PC)); 
                }
            });
            if (!pcPolicy.isPresent()) {
                // add availability details for first policy in list
                addAvailabilityDetailsToOndemand(ondemandElement, policies.get(0));
                addAudioStreamElement(ondemandElement, version, service);
            } else {
                addAvailabilityDetailsToOndemand(ondemandElement, pcPolicy.get());
                Policy ios3G = null;
                Policy iosWifi = null;
                for (Policy policy : policies) {
                    if (policy.getNetwork() !=  null) {
                        if (policy.getPlatform().equals(Platform.IOS) 
                                && policy.getNetwork().equals(Network.THREE_G)) {
                            ios3G = policy;
                        }
                        if (policy.getPlatform().equals(Platform.IOS) 
                                && policy.getNetwork().equals(Network.WIFI)) {
                            iosWifi = policy;
                        }
                    }
                }
                // if there are policies for both IOS-3G and IOS-Wifi, and both have actualAvailabilityStarts, and both of those times are before now, 
                // add the audiostreamgroup
                if (ios3G != null && iosWifi != null) {
                    if (ios3G.getActualAvailabilityStart() != null && iosWifi.getActualAvailabilityStart() != null) {
                        if (ios3G.getActualAvailabilityStart().isBefore(new DateTime()) && iosWifi.getActualAvailabilityStart().isBefore(new DateTime())) {
                            addAudioStreamElement(ondemandElement, version, service);
                        }
                    }
                }
            }
        }
           

        return ondemandElement;
    }
    
    private void addAvailabilityDetailsToOndemand(Element ondemandElement, Policy policy) {
        DateTime availableTill = Ordering.natural().min(policy.getAvailabilityEnd(), MAX_AVAILABLE_TILL);
        DateTime availableFrom = policy.getAvailabilityStart();
        if (availableTill != null && availableFrom != null) {
            Element availabilityElem = createElement("availability", RADIOPLAYER);
            Element availabilityScopeElem = createElement("scope", RADIOPLAYER);
            availabilityScopeElem.addAttribute(new Attribute("startTime", DATE_TIME_FORMAT.print(availableFrom)));
            availabilityScopeElem.addAttribute(new Attribute("stopTime", DATE_TIME_FORMAT.print(availableTill)));
            availabilityElem.appendChild(availabilityScopeElem);
            ondemandElement.appendChild(availabilityElem);
        }
    }
    
    private void addAudioStreamElement(Element ondemandElement, Version version, RadioPlayerService service) {
        if (RadioPlayerServices.nationalNetworks.contains(service) && Strings.emptyToNull(version.getCanonicalUri()) != null) {
            ondemandElement.appendChild(audioStreamGroupElement(version));
        }
    }

	private Element audioStreamGroupElement(Version version) {
        Element audioStreamGroupElem = createElement("audioStreamGroup", RADIOPLAYER);
        Element audioStreamElem = createElement("audioStream", RADIOPLAYER);
        
        Element audioSourceElem = createElement("audioSource", RADIOPLAYER);
        audioSourceElem.addAttribute(new Attribute("url", audioSourceUrl(version.getCanonicalUri())));
        audioSourceElem.addAttribute(new Attribute("mimeValue", "application/vnd.bbc-mediaselector+json"));
        audioStreamElem.appendChild(audioSourceElem);
        
        Element audioFormatElem = createElement("audioFormat", RADIOPLAYER);
        audioFormatElem.addAttribute(new Attribute("href","urn:mpeg:mpeg7:cs:AudioPresentationCS:2001:3"));
        audioStreamElem.appendChild(audioFormatElem);
        
        Element bitRateElem = createElement("bitRate", RADIOPLAYER);
        bitRateElem.addAttribute(new Attribute("target","128000"));
        bitRateElem.addAttribute(new Attribute("variable","true"));
        audioStreamElem.appendChild(bitRateElem);
        
        audioStreamGroupElem.appendChild(audioStreamElem);
        return audioStreamGroupElem;
    }

    private String audioSourceUrl(String versionUri) {
        return String.format(
            "http://open.live.bbc.co.uk/mediaselector/5/select/version/2.0/proto/http/format/json/vpid/%s/mediaset/",
            versionUri.replace("http://www.bbc.co.uk/programmes/","")
        );
    }

    @SuppressWarnings("unused")
	private void addRestriction(Element ondemandElement, Country country) {
		if (!Countries.ALL.equals(country)) {
			Element restrictionElem = createElement("restriction", RADIOPLAYER);
			restrictionElem.addAttribute(new Attribute("relationship", "allow"));
			restrictionElem.appendChild(country.code());
			ondemandElement.appendChild(restrictionElem);
		} else {
			Element restrictionElem = createElement("restriction", RADIOPLAYER);
			restrictionElem.addAttribute(new Attribute("relationship", "deny"));
			ondemandElement.appendChild(restrictionElem);
		}
	}

    private Element scopeElement(LocalDate day, RadioPlayerService id) {
        Element scope = createElement("scope", EPGSCHEDULE);
        scope.addAttribute(new Attribute("startTime", DATE_TIME_FORMAT.print(day.toDateTimeAtStartOfDay(DateTimeZones.UTC))));
        scope.addAttribute(new Attribute("stopTime", DATE_TIME_FORMAT.print(day.toDateTimeAtStartOfDay(DateTimeZones.UTC).plusDays(1))));

        Element service = createElement("serviceScope", EPGSCHEDULE);
        service.addAttribute(new Attribute("id", id.getDabServiceId().replaceAll("_", ".")));
        service.addAttribute(new Attribute("radioplayerId", String.valueOf(id.getRadioplayerId())));
        scope.appendChild(service);
        return scope;
    }
}
