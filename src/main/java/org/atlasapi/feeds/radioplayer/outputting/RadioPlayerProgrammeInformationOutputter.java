package org.atlasapi.feeds.radioplayer.outputting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import nu.xom.Attribute;
import nu.xom.Element;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerPiFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.format.ISOPeriodFormat;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.SystemClock;

public class RadioPlayerProgrammeInformationOutputter extends RadioPlayerXMLOutputter {

    private static final String ORIGINATOR = "Metabroadcast";

    private final RadioPlayerGenreElementCreator genreElementCreator;
    
    private static final Ordering<Interval> orderByStartThenEnd
        = Ordering.from(new Comparator<Interval>() {
            @Override
            public int compare(Interval left, Interval right) {
                return ComparisonChain.start()
                        .compare(left.getStart(), right.getStart())
                        .compare(left.getEnd(), right.getEnd())
                        .result();
            }
        });

    private final Clock clock;
    
    public RadioPlayerProgrammeInformationOutputter(RadioPlayerGenreElementCreator genreElementCreator) {
        this(new SystemClock(), genreElementCreator);
    }
    
    public RadioPlayerProgrammeInformationOutputter(Clock clock, RadioPlayerGenreElementCreator genreElementCreator) {
        super();
        this.genreElementCreator = genreElementCreator;
        this.clock = checkNotNull(clock);
    }

    @Override
    public Element createFeed(RadioPlayerFeedSpec spec, Iterable<RadioPlayerBroadcastItem> items) {
        Preconditions.checkArgument(spec instanceof RadioPlayerPiFeedSpec);
        
        DateTime now = clock.now();
        
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
        schedule.addAttribute(new Attribute("creationTime", DATE_TIME_FORMAT.print(now)));

        schedule.appendChild(scopeElement(piSpec.getDay(), piSpec.getService()));

        for (RadioPlayerBroadcastItem item : items) {
            schedule.appendChild(createProgrammeElement(item, piSpec.getService(), now));
        }

        epgElem.appendChild(schedule);
        return epgElem;
    }

    private Element createProgrammeElement(RadioPlayerBroadcastItem broadcastItem, RadioPlayerService id, DateTime now) {
        Element programme = createElement("programme", EPGSCHEDULE);
        programme.addAttribute(new Attribute("shortId", "0"));
        programme.addAttribute(new Attribute("id", createCridFromUri(broadcastItem.getItem().getCanonicalUri())));

        String title = itemTitle(broadcastItem);
        programme.appendChild(stringElement("mediumName", EPGDATATYPES, MEDIUM_TITLE.truncatePossibleNull(title)));
        programme.appendChild(stringElement("longName", EPGDATATYPES, LONG_TITLE.truncatePossibleNull(title)));

        Broadcast broadcast = broadcastItem.getBroadcast();
        programme.appendChild(locationElement(broadcast, id));
        
        String desc = broadcastItem.getItem().getMediumDescription();
        desc = desc == null ? broadcastItem.getItem().getDescription() : desc;
        programme.appendChild(mediaDescription(stringElement("shortDescription", EPGDATATYPES, SHORT_DESC.truncatePossibleNull(desc))));
        if (!Strings.isNullOrEmpty(broadcastItem.getItem().getImage())) {
            for (ImageDimensions dimensions : imageDimensions) {
                Element img = createImageDescriptionElem(broadcastItem.getItem(), dimensions);
                programme.appendChild(mediaDescription(img));
            }
        }

        for (Element genreElement : genreElementCreator.genreElementsFor(broadcastItem.getItem())) {
            programme.appendChild(genreElement);
        }

        //Because outputCountries always contains ALL, international block output is suppressed.
        ListMultimap<Country, Location> locationsByCountry = ArrayListMultimap.create();
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
                List<Location> countryLocations = locationsByCountry.get(country);
                ListMultimap<Interval, Location> availabilityIndex = availabilityIndex(countryLocations);
                availabilityIndex = mergeOverlappingAvailabilities(availabilityIndex);
                if (!availabilityIndex.isEmpty()) {
                    Entry<Interval, List<Location>> windowAndlocations = locationsClosestAfterNow(availabilityIndex, now);
                    Interval window = windowAndlocations.getKey();
                    List<Location> location = windowAndlocations.getValue();
                    Element ode = ondemandElement(broadcastItem.getItem(), broadcastItem.getVersion(), window, location, id);
                    programme.appendChild(ode);
                }
            }
        }
        
        return programme;
    }
    
    private Entry<Interval,List<Location>> locationsClosestAfterNow(ListMultimap<Interval, Location> availabilityIndex, DateTime now) {
        Preconditions.checkArgument(!availabilityIndex.isEmpty());
        ImmutableList<Interval> windows = orderByStartThenEnd.immutableSortedCopy(availabilityIndex.keySet());
        Interval window = null;
        for (Interval current : windows) {
            window = current;
            if (window.contains(now) || window.isAfter(now)) {
                break;
            }
        }
        return Maps.immutableEntry(window, availabilityIndex.get(window));
    }

    private ListMultimap<Interval, Location> mergeOverlappingAvailabilities(
            ListMultimap<Interval, Location> unmerged) {
        ImmutableList<Interval> windows = orderByStartThenEnd.immutableSortedCopy(unmerged.keySet());
        ImmutableListMultimap.Builder<Interval, Location> merged = ImmutableListMultimap.builder();
        int i = 0;
        while(i < windows.size()) {
            Interval current = windows.get(i);
            DateTime start = current.getStart();
            ImmutableList.Builder<Location> overlappingLocations = ImmutableList.builder();
            overlappingLocations.addAll(unmerged.get(current));
            int j = i+1;
            while(j < windows.size()) {
                Interval next =  windows.get(j);
                if (next.overlaps(current)) {
                    overlappingLocations.addAll(unmerged.get(next));
                    current = next;
                    j++;
                } else {
                    break;
                }
            }
            i = j;
            DateTime end = current.getEnd();
            merged.putAll(new Interval(start, end), overlappingLocations.build());
        }
        return merged.build();
    }

    private ListMultimap<Interval, Location> availabilityIndex(List<Location> locations) {
        ImmutableListMultimap.Builder<Interval, Location> index
            = ImmutableListMultimap.builder();
        for (Location location : locations) {
            Policy policy = location.getPolicy();
            if (policy != null) {
                DateTime availabilityStart = policy.getAvailabilityStart();
                DateTime availabilityEnd = availabilityEndOrMax(policy);
                if (availabilityStart != null && availabilityEnd  != null) {
                    index.put(new Interval(availabilityStart, availabilityEnd), location);
                }
            }
        }
        return index.build();
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
