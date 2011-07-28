package org.atlasapi.feeds.lakeview;

import static java.lang.Boolean.TRUE;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Document;
import nu.xom.Element;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Channel;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;

public class LakeviewFeedCompiler {

    private static final Ordering<Broadcast> TRANSMISSION_ORDERING = Ordering.from(new Comparator<Broadcast>() {
                @Override
                public int compare(Broadcast o1, Broadcast o2) {
                    return o1.getTransmissionTime().compareTo(o2.getTransmissionTime());
                }
            });
    private static final DateTimeFormatter DATETIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
    private static final XMLNamespace LAKEVIEW = new XMLNamespace("", "http://schemas.microsoft.com/Lakeview/2011/06/13/ingestion");

    private final Clock clock;

    public LakeviewFeedCompiler(Clock clock) {
        this.clock = clock;
    }

    public LakeviewFeedCompiler() {
        this(new SystemClock());
    }

    public Document compile(List<LakeviewContentGroup> contents) {

        Element feed = createElement("Feed", LAKEVIEW);

        String lastModified = DATETIME_FORMAT.print(clock.now());

        for (LakeviewContentGroup content : contents) {
            Iterable<Episode> episodes = Iterables.concat(content.contents().values());
            feed.appendChild(createBrandElem(content.brand(), lastModified, episodes, content.contents().keySet().size()));
            
            for (Entry<Series, Collection<Episode>> seriesEpisodes : content.contents().entrySet()) {
                feed.appendChild(createSeriesElem(seriesEpisodes.getKey(), content.brand(), seriesEpisodes.getValue(), lastModified, hasGuidance(seriesEpisodes.getValue())));
                
                for (Episode episode : seriesEpisodes.getValue()) {
                    feed.appendChild(createEpisodeElem(episode, content.brand(), lastModified, hasGuidance(episode)));
                }
                
            }
        }

        return new Document(feed);
    }

    private boolean hasGuidance(Episode episode) {
        for (Version version : episode.getVersions()) {
            if (TRUE.equals(version.getRestriction().isRestricted())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGuidance(Iterable<Episode> episodes) {
        return Iterables.any(episodes, new Predicate<Episode>() {
            @Override
            public boolean apply(Episode input) {
                return hasGuidance(input);
            }
        });
    }

    private Element createBrandElem(Brand brand, String lastModified, Iterable<Episode> episodes, int seasons) {
        Element element = createElement("TVSeries", LAKEVIEW);
        element.appendChild(stringElement("ItemId", LAKEVIEW, brandId(brand.getCanonicalUri())));
        element.appendChild(stringElement("Title", LAKEVIEW, Strings.isNullOrEmpty(brand.getTitle()) ? "EMPTY SERIES TITLE" : brand.getTitle()));
        
        appendCommonElements(element, brand, lastModified, hasGuidance(episodes));
        
        List<Broadcast> broadcasts = extractBroadcasts(episodes);
        appendOriginalBroadcastElement(broadcasts, element);
        
        element.appendChild(stringElement("TotalNumberOfSeasons", LAKEVIEW, String.valueOf(seasons)));
        if(!broadcasts.isEmpty()) {
            element.appendChild(stringElement("Network", LAKEVIEW, extractNetwork(broadcasts)));
            element.appendChild(stringElement("EndYear", LAKEVIEW, extractEndYear(broadcasts)));
        }
        
        return element;
    }

    private String extractEndYear(Iterable<Broadcast> broadcasts) {
        return String.valueOf(TRANSMISSION_ORDERING.max(broadcasts).getTransmissionEndTime().getYear());
    }

    private String extractNetwork(List<Broadcast> broadcasts) {
        return Channel.fromUri(TRANSMISSION_ORDERING.min(broadcasts).getBroadcastOn()).requireValue().title();
    }

    private Element createSeriesElem(Series series, Brand parent, Iterable<Episode> episodes, String lastModified, boolean hasGuidance) {
        Element element = createElement("TVSeason", LAKEVIEW);
        element.appendChild(stringElement("ItemId", LAKEVIEW, seriesId(series.getCanonicalUri())));
        
        if(Strings.isNullOrEmpty(series.getTitle()) || series.getTitle().matches("(?i)series \\d+")) {
            element.appendChild(stringElement("Title", LAKEVIEW, String.format("%s Series %s", parent.getTitle(), series.getSeriesNumber())));
        } else {
            element.appendChild(stringElement("Title", LAKEVIEW, series.getTitle()));
        }
        
        appendCommonElements(element, series, lastModified, hasGuidance);
        
        appendOriginalBroadcastElement(extractBroadcasts(episodes), element);
        element.appendChild(stringElement("SeasonNumber", LAKEVIEW, String.valueOf(((Series) series).getSeriesNumber())));
        element.appendChild(stringElement("SeriesId", LAKEVIEW, brandId(((Series) series).getParent().getUri())));
        
        return element;
    }

    private Element createEpisodeElem(Episode episode, Brand container, String lastModified, boolean hasGuidance) {
        Element element = createElement("TVEpisode", LAKEVIEW);
        element.appendChild(stringElement("ItemId", LAKEVIEW, episodeId(episode.getCanonicalUri())));
        
        if(Strings.isNullOrEmpty(episode.getTitle()) || episode.getTitle().matches("(?i)(series \\d+)? episode \\d+")) {
            element.appendChild(stringElement("Title", LAKEVIEW, String.format("%s Series %s Episode %s", container.getTitle(), episode.getSeriesNumber(), episode.getEpisodeNumber())));
        } else {
            element.appendChild(stringElement("Title", LAKEVIEW, episode.getTitle()));
        }
        
        appendCommonElements(element, episode, lastModified, hasGuidance);

        element.appendChild(stringElement("ApplicationSpecificData", LAKEVIEW, extract4OdId(episode)));
        
        List<Broadcast> broadcasts = extractBroadcasts(ImmutableList.of(episode));
        if(!broadcasts.isEmpty()) {
            element.appendChild(stringElement("OriginalPublicationDate", LAKEVIEW, extractFirstBroadcastDate(broadcasts)));
        } else {
            String fad = extractFirstAvailabilityDate(episode);
            element.appendChild(stringElement("OriginalPublicationDate", LAKEVIEW, fad == null ? new DateTime(0).toString(DATETIME_FORMAT) : fad));
        }
        
        element.appendChild(stringElement("EpisodeNumber", LAKEVIEW, String.valueOf(episode.getEpisodeNumber())));
        element.appendChild(stringElement("DurationInSeconds", LAKEVIEW, String.valueOf(duration(episode))));
        element.appendChild(stringElement("SeriesId", LAKEVIEW, brandId(episode.getContainer().getUri())));
        element.appendChild(stringElement("SeasonId", LAKEVIEW, seriesId(episode.getSeriesRef().getUri())));
        
        return element;
    }

    private String extractFirstAvailabilityDate(Episode episode) {
        for (Version version : episode.getVersions()) {
            for (Encoding encoding : version.getManifestedAs()) {
                for (Location location : encoding.getAvailableAt()) {
                    if(location.getPolicy() != null && location.getPolicy().getAvailabilityStart() != null) {
                        return location.getPolicy().getAvailabilityStart().toString(DATETIME_FORMAT);
                    }
                }
            }
        }
        return null;
    }

    private void appendOriginalBroadcastElement(List<Broadcast> broadcasts, Element element) {
        if(!broadcasts.isEmpty()) {
            element.appendChild(stringElement("OriginalPublicationDate", LAKEVIEW, extractFirstBroadcastDate(broadcasts)));
        }
    }
    
    private String extractFirstBroadcastDate(List<Broadcast> broadcasts) {
        return TRANSMISSION_ORDERING.min(broadcasts).getTransmissionTime().toString(DATETIME_FORMAT);
    }

    private List<Broadcast> extractBroadcasts(Iterable<Episode> episodes) {
        List<Broadcast> broadcasts = Lists.newArrayList();
        for (Episode episode : episodes) {
            for (Version version : episode.getVersions()) {
                broadcasts.addAll(version.getBroadcasts());
            }
        }
        return broadcasts;
    }

    private String extract4OdId(Episode episode) {
        for (Version version : episode.getVersions()) {
            for (Encoding encoding : version.getManifestedAs()) {
                for (Location location : encoding.getAvailableAt()) {
                    if(location.getTransportType().equals(TransportType.LINK)) {
                        Pattern pattern = Pattern.compile(".*#(\\d+)$");
                        Matcher matcher = pattern.matcher(location.getUri());
                        if(matcher.matches()) {
                            return matcher.group(1);                            
                        }
                    }
                }
            }
        }
        return "NONE";
    }

    private void appendCommonElements(Element element, Content content, String lastModified, boolean hasGuidance) {
        
        if(!Strings.isNullOrEmpty(content.getDescription())) {
            element.appendChild(stringElement("Description", LAKEVIEW, content.getDescription()));
        }
        
        element.appendChild(stringElement("IsUserGenerated", LAKEVIEW, "false"));
        
        if(content instanceof Brand && content.getImage() != null) {
            
            Element imageElem = createElement("Image", LAKEVIEW);
            imageElem.appendChild(stringElement("ImagePurpose", LAKEVIEW, "BoxArt"));
            imageElem.appendChild(stringElement("Url", LAKEVIEW, content.getImage()));
            
            Element imagesElement = createElement("Images", LAKEVIEW);
            imagesElement.appendChild(imageElem);
            element.appendChild(imagesElement);
        }
        
        element.appendChild(stringElement("LastModifiedDate", LAKEVIEW, lastModified));
        
        if(!content.getGenres().isEmpty()) {
            Element genres = createElement("Genres", LAKEVIEW);
            for (String genre : content.getGenres()) {
                if(genre.startsWith("http://www.channel4.com")) {
                    genres.appendChild(stringElement("Genre", LAKEVIEW, genre));
                }
            }
            element.appendChild(genres);
        }
        
        Element pc = createElement("ParentalControl", LAKEVIEW);
        pc.appendChild(stringElement("HasGuidance", LAKEVIEW, String.valueOf(hasGuidance)));
        element.appendChild(pc);
        element.appendChild(stringElement("PublicWebUri", LAKEVIEW, content.getCanonicalUri()));
    }

    private static final String ID_PREFIX = "http://channel4.com/en-GB";
    private static final String C4_PROG_BASE = "http://www.channel4.com/programmes/";
    
    private String brandId(String brandUri) {
        return String.format("%s/TVSeries/%s", ID_PREFIX, brandUri.replaceAll(C4_PROG_BASE, ""));
    }

    private String seriesId(String seriesUri) {
        return String.format("%s/TVSeason/%s", ID_PREFIX, seriesUri.replaceAll(C4_PROG_BASE, "").replaceAll("/episode-guide/", "-"));
    }

    private String episodeId(String episodeUri) {
        return String.format("%s/TVEpisode/%s", ID_PREFIX, episodeUri.replaceAll(C4_PROG_BASE, "").replaceAll("/episode-guide/(series-\\d+)/(episode-\\d+)", "-$1-$2"));
    }

    private Integer duration(Episode episode) {
        for (Version version : episode.getVersions()) {
            Integer duration = version.getDuration();
            if (duration != null) {
                return duration;
            }
        }
        return null;
    }

    protected Element stringElement(String name, XMLNamespace ns, String value) {
        Element elem = createElement(name, ns);
        elem.appendChild(value);
        return elem;
    }

    protected Element createElement(String name, XMLNamespace ns) {
        Element elem = new Element(name, ns.getUri());
        if (!LAKEVIEW.equals(ns)) {
            elem.setNamespacePrefix(ns.getPrefix());
        }
        return elem;
    }

}
