package org.atlasapi.feeds.sitemaps;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.TransportSubType;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.collect.ImmutableOptionalMap;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;
import com.metabroadcast.common.text.Truncator;
import com.metabroadcast.common.time.DateTimeZones;

public class SiteMapOutputter {

    private static final DateTimeFormatter DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();

    private static final XMLNamespace SITEMAP = new XMLNamespace("sitemap", "http://www.sitemaps.org/schemas/sitemap/0.9");
    private static final XMLNamespace VIDEO = new XMLNamespace("video", "http://www.google.com/schemas/sitemap-video/1.1");

    private static final Truncator descTruncator = new Truncator().onlyTruncateAtAWordBoundary().withMaxLength(2048);
    private static final Truncator titleTruncator = new Truncator().onlyTruncateAtAWordBoundary().withMaxLength(100);
    
    private final Map<Publisher, Optional<SiteMapUriGenerator>> publisherSpecificUriGenerators;
    private final SiteMapUriGenerator defaultPublisherSpecificSitemapUriGenerator;

    public SiteMapOutputter(Map<Publisher, SiteMapUriGenerator> publisherSpecificUriGenerators, 
            SiteMapUriGenerator defaultPublisherSpecificSitemapUriGenerator) {
        
        this.defaultPublisherSpecificSitemapUriGenerator = checkNotNull(defaultPublisherSpecificSitemapUriGenerator);
        this.publisherSpecificUriGenerators = ImmutableOptionalMap.fromMap(publisherSpecificUriGenerators);
    }
    
    public void output(Map<ParentRef, Container> parentLookup, Iterable<Content> feed, OutputStream out) throws IOException {
        Element feedElem = createFeed(feed, parentLookup);
        write(out, feedElem);
    }

    public void outputBrands(Iterable<Brand> brands, String format, ServletOutputStream out) throws IOException {
        Element feedElem = createFeedOfBrands(brands, format);
        write(out, feedElem);
    }

    private Element createFeedOfBrands(Iterable<Brand> brands, String format) {
        Element feed = new Element("urlset", SITEMAP.getUri());
        for (Brand brand : brands) {
            Element urlElement = createElement("url", SITEMAP);
            urlElement.appendChild(stringElement("loc", SITEMAP, format.replace("{curie}", brand.getCurie())));
            feed.appendChild(urlElement);
        }
        return feed;
    }

    private Element createFeed(Iterable<Content> contents, Map<ParentRef, Container> parentLookup) {
        Element feed = new Element("urlset", SITEMAP.getUri());
        VIDEO.addDeclarationTo(feed);
        for (Content content : Iterables.concat(contents, parentLookup.values())) {
            if (content instanceof Item) {
                Item item = (Item) content;
                entryForItem(feed, item, itemTitle(item, parentLookup));
            }
            for (Clip clip : content.getClips()) {
                entryForClip(feed, content, clip, clipTitle(clip, content, parentLookup));
            }
        }
        return feed;
    }

    private void entryForItem(Element feed, Item item, String title) {
        Location location = locationFrom(item, Optional.<TransportSubType>absent());
        if (location != null && item.getThumbnail() != null) {
            
            SiteMapUriGenerator uriGenerator = publisherSpecificUriGenerators
                    .get(item.getPublisher())
                    .or(defaultPublisherSpecificSitemapUriGenerator);
            feed.appendChild(urlEntry(
                    uriGenerator.playerPageUriForContent(item, location),
                    uriGenerator.videoUriForContent(item, location),
                    item, location, title));
        }
    }
    
    private void entryForClip(Element feed, Content content, Clip clip, String title) {
        Location location = locationFrom(clip, Optional.of(TransportSubType.BRIGHTCOVE));
        if (location != null && content.getThumbnail() != null) {
            SiteMapUriGenerator uriGenerator = publisherSpecificUriGenerators
                    .get(content.getPublisher())
                    .or(defaultPublisherSpecificSitemapUriGenerator);
            
            feed.appendChild(urlEntry(
                    uriGenerator.playerPageUriForClip(content, clip, location),
                    uriGenerator.videoUriForClip(clip, location),
                    clip, location, title));
        }
    }

    private Element urlEntry(Optional<String> playerPageUri, Optional<String> videoUri, Item item, Location location, String title) {
        Element urlElement = createElement("url", SITEMAP);
        
        if (playerPageUri.isPresent()) {
            urlElement.appendChild(stringElement("loc", SITEMAP, playerPageUri.get()));
        }
                
        if (item.getLastUpdated() != null) {
            urlElement.appendChild(stringElement("lastmod", SITEMAP, DATE_TIME_FORMAT.print(item.getLastUpdated())));
        }
        if (location.getAvailable()) {
            urlElement.appendChild(videoElem(videoUri, item, location, title));
        }
        return urlElement;
    }
    

    private Element videoElem(Optional<String> videoUri, Item item, Location location, String title) {
        Element videoElem = createElement("video", VIDEO);
        videoElem.appendChild(stringElement("thumbnail_loc", VIDEO, item.getThumbnail()));
        videoElem.appendChild(stringElement("title", VIDEO, title));
        videoElem.appendChild(stringElement("description", VIDEO, descTruncator.truncatePossibleNull(item.getDescription())));
        
        if (videoUri.isPresent()) {
            Element playerLocation = stringElement("player_loc", VIDEO, videoUri.get());
            playerLocation.addAttribute(new Attribute("allow_embed", "no"));
            videoElem.appendChild(playerLocation);
        }
        
        Integer duration = getDuration(item);
        if (duration != null) {
            videoElem.appendChild(stringElement("duration", VIDEO, String.valueOf(duration)));
        }

        Policy policy = location.getPolicy();
        if (policy != null) {
            DateTime availableTill = policy.getAvailabilityEnd();
            if (availableTill != null && availableTill.isBefore(new DateTime(DateTimeZones.UTC).plusYears(10))) {
                videoElem.appendChild(stringElement("expiration_date", VIDEO, DATE_TIME_FORMAT.print(availableTill)));
            }
            Set<Country> countries = policy.getAvailableCountries();
            if (countries != null && !countries.contains(Countries.ALL)) {
                String spaceDelimted = Joiner.on(' ').join(Iterables.transform(countries, Country.UNPACK_COUNTRY_CODE));
                Element restrictionElem = createElement("restriction", VIDEO);
                restrictionElem.addAttribute(new Attribute("relationship", "allow"));
                restrictionElem.appendChild(spaceDelimted);
                videoElem.appendChild(restrictionElem);
            }

        }
        return videoElem;
    }

    private String itemTitle(Item item, Map<ParentRef, Container> parentLookup) {
        String title = Strings.nullToEmpty(item.getTitle());
        Container parent = parentLookup.get(item.getContainer());
        if (parent != null && !Strings.isNullOrEmpty(parent.getTitle())) {
            String brandTitle = parent.getTitle();
            if (!brandTitle.equals(title)) {
                title = brandTitle + " : " + title;
            }
        }
        return titleTruncator.truncate(title);
    }
    
    private String clipTitle(Clip clip, Content content, Map<ParentRef, Container> parentLookup) {
        String title = Strings.nullToEmpty(clip.getTitle());
        
        Container topLevelContainer = null;
        if (content instanceof Brand) {
            topLevelContainer = (Brand) content;
        } else if (content instanceof Series) {
            topLevelContainer = parentLookup.get(((Series)content).getParent());
        } else if (content instanceof Item) {
            topLevelContainer = parentLookup.get(((Item)content).getContainer());
        }
        
        if (topLevelContainer != null 
                && !Strings.isNullOrEmpty(topLevelContainer.getTitle())) {
            String brandTitle = topLevelContainer.getTitle();
            if (!brandTitle.equals(title)) {
                title = brandTitle + " : " + title;
            }
        }
        return titleTruncator.truncate(title);
    }

    private Integer getDuration(Item item) {
        for (Version version : item.nativeVersions()) {
            return version.getDuration();
        }
        return null;
    }

    private Location locationFrom(Item item, Optional<TransportSubType> subType) {
        for (Version version : item.nativeVersions()) {
            for (Encoding encoding : version.getManifestedAs()) {
                for (Location location : encoding.getAvailableAt()) {
                    if (isPcOrNullPlatformLinkLocation(location)
                            && (!subType.isPresent() || subType.get().equals(location.getTransportSubType()))) {
                        return location;
                    }
                }
            }
        }
        return null;
    }

    private boolean isPcOrNullPlatformLinkLocation(Location location) {
        boolean isLinkLocation = TransportType.LINK.equals(location.getTransportType());
        Policy policy = location.getPolicy();
        if (policy == null) {
            return isLinkLocation;
        }
        return isLinkLocation && (policy.getPlatform() == null || Platform.PC.equals(policy.getPlatform()));
    }

    private void write(OutputStream out, Element feed) throws UnsupportedEncodingException, IOException {
        Serializer serializer = new Serializer(out, Charsets.UTF_8.toString());
        serializer.setIndent(4);
        serializer.setLineSeparator("\n");
        serializer.write(new Document(feed));
    }

    private Element stringElement(String name, XMLNamespace ns, String value) {
        Element elem = createElement(name, ns);
        elem.appendChild(value);
        return elem;
    }

    private Element createElement(String name, XMLNamespace ns) {
        Element elem = new Element(name, ns.getUri());
        if (!SITEMAP.equals(ns)) {
            elem.setNamespacePrefix(ns.getPrefix());
        }
        return elem;
    }

}
