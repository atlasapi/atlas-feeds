package org.atlasapi.feeds.radioplayer.outputting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.entity.Policy.Network;
import org.atlasapi.media.entity.Policy.Platform;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;
import com.metabroadcast.common.text.Truncator;
import com.metabroadcast.common.time.DateTimeZones;

public abstract class RadioPlayerXMLOutputter {

    private static final String ONDEMAND_LOCATION = "http://www.bbc.co.uk/radio/player/";
    private static final DateTime MAX_AVAILABLE_TILL = new DateTime(2037, 01, 01, 0, 0, 0, 0, DateTimeZones.UTC);
    
    protected static final DateTimeFormatter DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
    protected static final Truncator MEDIUM_TITLE = new Truncator().withMaxLength(16).onlyTruncateAtAWordBoundary().omitTrailingPunctuationWhenTruncated();
    protected static final Truncator LONG_TITLE = new Truncator().withMaxLength(128).onlyTruncateAtAWordBoundary().omitTrailingPunctuationWhenTruncated();
    protected static final Truncator SHORT_DESC = new Truncator().withMaxLength(180).onlyTruncateAtAWordBoundary().omitTrailingPunctuationWhenTruncated();
    protected static final XMLNamespace EPGSCHEDULE = new XMLNamespace("", "http://www.radioplayer.co.uk/schemas/11/epgSchedule");
    protected static final XMLNamespace EPGDATATYPES = new XMLNamespace("epg", "http://www.radioplayer.co.uk/schemas/11/epgDataTypes");
    protected static final XMLNamespace XSI = new XMLNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    protected static final XMLNamespace RADIOPLAYER = new XMLNamespace("radioplayer", "http://www.radioplayer.co.uk/schemas/11/rpDataTypes");
    protected static final String SCHEMALOCATION = "http://www.radioplayer.co.uk/schemas/11/epgSchedule http://www.radioplayer.co.uk/schemas/10/epgSchedule_11.xsd";
    
    protected static final class ImageDimensions {

        private static final ImageDimensions valueOf(int width, int height) {
            return new ImageDimensions(String.valueOf(width), String.valueOf(height));
        }
        
        private final String width;
        private final String height;
        
        public ImageDimensions(String width, String height) {
            this.width = checkNotNull(width);
            this.height = checkNotNull(height);
        }

        public String getWidth() {
            return width;
        }
        
        public String getHeight() {
            return height;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(width, height);
        }
        
        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that instanceof ImageDimensions) {
                ImageDimensions other = (ImageDimensions) that;
                return width.equals(other.width)
                    && height.equals(other.height);
            }
            return false;
        }
        
        @Override
        public String toString() {
            return String.format("%sx%s", width, height);
        }
    }
    
    protected final ImmutableList<ImageDimensions> imageDimensions
        = ImmutableList.of(
            ImageDimensions.valueOf(86, 48),
            ImageDimensions.valueOf(288, 162),
            ImageDimensions.valueOf(160, 90)
        );
    
    protected abstract Element createFeed(RadioPlayerFeedSpec spec, Iterable<RadioPlayerBroadcastItem> items);

    public RadioPlayerXMLOutputter() {
        super();
    }

    public void output(RadioPlayerFeedSpec spec, Iterable<RadioPlayerBroadcastItem> items, OutputStream out) throws IOException {
        write(out, createFeed(spec, items));
    }

    private void write(OutputStream out, Element feed) throws UnsupportedEncodingException, IOException {
        Serializer serializer = new Serializer(out, Charsets.UTF_8.toString());
        serializer.setLineSeparator("\n");
        serializer.write(new Document(feed));
    }

    protected Element stringElement(String name, XMLNamespace ns, String value) {
        Element elem = createElement(name, ns);
        elem.appendChild(value);
        return elem;
    }

    protected Element createElement(String name, XMLNamespace ns) {
        Element elem = new Element(name, ns.getUri());
        if (!EPGSCHEDULE.equals(ns)) {
            elem.setNamespacePrefix(ns.getPrefix());
        }
        return elem;
    }

    protected Version versionFrom(Item item) {
        for (Version version : item.getVersions()) {
            if (hasLocation(version) && hasBroadcast(version)) {
                return version;
            }
        }
        return null;
    }
    
    protected Element ondemandElement(Item item, Version version, Interval window, Collection<Location> locations, RadioPlayerService service) {
        
        Element ondemandElement = createElement("ondemand", EPGDATATYPES);

        ondemandElement.appendChild(stringElement("player", RADIOPLAYER, ONDEMAND_LOCATION + item.getCanonicalUri().substring(item.getCanonicalUri().lastIndexOf("/") + 1)));

        // get the list of non-null policies from the provided list of locations
        List<Policy> policies = Lists.newArrayList(Iterables.filter(
                Iterables.transform(locations, new Function<Location, Policy>() {
                        @Override
                        public Policy apply(Location input) {
                            return input.getPolicy();
                        }
                }), 
                Predicates.notNull()
        ));
        
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
                addAvailabilityDetailsToOndemand(ondemandElement, window == null ? window(policies.get(0)) : window);
                addAudioStreamElement(ondemandElement, version, service);
            } else {
                addAvailabilityDetailsToOndemand(ondemandElement, window == null ? window(pcPolicy.get()) : window);
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

    private Interval window(Policy policy) {
        DateTime availabilityStart = policy.getActualAvailabilityStart() != null ? policy.getActualAvailabilityStart() : policy.getAvailabilityStart();
        return new Interval(availabilityStart, availabilityEndOrMax(policy));
    }

    private void addAvailabilityDetailsToOndemand(Element ondemandElement, Interval window) {
        DateTime availableTill = window.getEnd();
        DateTime availableFrom = window.getStart();
        if (availableTill != null && availableFrom != null) {
            Element availabilityElem = createElement("availability", RADIOPLAYER);
            Element availabilityScopeElem = createElement("scope", RADIOPLAYER);
            availabilityScopeElem.addAttribute(new Attribute("startTime", DATE_TIME_FORMAT.print(availableFrom)));
            availabilityScopeElem.addAttribute(new Attribute("stopTime", DATE_TIME_FORMAT.print(availableTill)));
            availabilityElem.appendChild(availabilityScopeElem);
            ondemandElement.appendChild(availabilityElem);
        }
    }

    protected DateTime availabilityEndOrMax(Policy policy) {
        return Ordering.natural().nullsLast().min(policy.getAvailabilityEnd(), MAX_AVAILABLE_TILL);
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
            versionUri.replaceAll("http://[a-z]*.bbc.co.uk/programmes/","")
        );
    }
    
    protected Set<Country> representedBy(Encoding encoding, Location location) {
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

    private boolean hasLocation(Version version) {
        for (Encoding encoding : version.getManifestedAs()) {
            for (Location location : encoding.getAvailableAt()) {
                if (TransportType.LINK.equals(location.getTransportType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasBroadcast(Version version) {
        return version.getBroadcasts().size() > 0;
    }

    protected Location locationFrom(Item item) {
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
    
    protected String createCridFromUri(String uri) {
        return uri.replaceAll("http://[a-z]*\\.bbc\\.co\\.uk", "crid://www\\.bbc\\.co\\.uk");
    }
    

    protected Element createImageDescriptionElem(Item item, ImageDimensions dimensions) {
        return createImageDescriptionElem(item, dimensions.getWidth(), dimensions.getHeight());
    }
    
    protected Element createImageDescriptionElem(Item item, String width, String height) {
        Element imageElement = createElement("multimedia", EPGDATATYPES);
        imageElement.addAttribute(new Attribute("mimeValue", "image/jpeg"));
        String location = generateImageLocationFrom(item, width, height);
        imageElement.addAttribute(new Attribute("url", location));
        imageElement.addAttribute(new Attribute("width", width));
        imageElement.addAttribute(new Attribute("height", height));
        return imageElement;
    }
    
    private String generateImageLocationFrom(Item item, String width, String height) {
        Pattern p = Pattern.compile("(.*/)\\d+x\\d+(/.*).jpg");
        Matcher m = p.matcher(item.getImage());
        if (m.matches()) {
            return String.format("%s%sx%s%s.jpg", m.group(1), width, height, m.group(2));
        }
        
        p = Pattern.compile("(.*)_\\d+_\\d+.jpg");
        m = p.matcher(item.getImage());
        if (m.matches()) {
            return String.format("%s_%s_%s.jpg", m.group(1), width, height);
        }
        
        
        return item.getImage();
    }

    protected Broadcast broadcastFrom(Item item, String broadcaster) {
        for (Version version : item.getVersions()) {
            for (Broadcast broadcast : version.getBroadcasts()) {
                if (broadcaster.equals(broadcast.getBroadcastOn())) {
                    return broadcast;
                }
            }
        }
        return null;
    }

}