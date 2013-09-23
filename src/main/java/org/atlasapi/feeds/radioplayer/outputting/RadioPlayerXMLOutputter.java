package org.atlasapi.feeds.radioplayer.outputting;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedSpec;
import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Charsets;
import com.metabroadcast.common.text.Truncator;

public abstract class RadioPlayerXMLOutputter {

    protected static final DateTimeFormatter DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();

    protected abstract Element createFeed(RadioPlayerFeedSpec spec, Iterable<RadioPlayerBroadcastItem> items);

    protected static final Truncator MEDIUM_TITLE = new Truncator().withMaxLength(16).onlyTruncateAtAWordBoundary().omitTrailingPunctuationWhenTruncated();
    protected static final Truncator LONG_TITLE = new Truncator().withMaxLength(128).onlyTruncateAtAWordBoundary().omitTrailingPunctuationWhenTruncated();
    protected static final Truncator SHORT_DESC = new Truncator().withMaxLength(180).onlyTruncateAtAWordBoundary().omitTrailingPunctuationWhenTruncated();

    protected static final XMLNamespace EPGSCHEDULE = new XMLNamespace("", "http://www.radioplayer.co.uk/schemas/11/epgSchedule");
    protected static final XMLNamespace EPGDATATYPES = new XMLNamespace("epg", "http://www.radioplayer.co.uk/schemas/11/epgDataTypes");
    protected static final XMLNamespace XSI = new XMLNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    protected static final XMLNamespace RADIOPLAYER = new XMLNamespace("radioplayer", "http://www.radioplayer.co.uk/schemas/11/rpDataTypes");
    protected static final String SCHEMALOCATION = "http://www.radioplayer.co.uk/schemas/11/epgSchedule http://www.radioplayer.co.uk/schemas/10/epgSchedule_11.xsd";

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
    
    protected Element createImageDescriptionElem(Item item) {
        Element imageElement = createElement("multimedia", EPGDATATYPES);
        imageElement.addAttribute(new Attribute("mimeValue", "image/jpeg"));
        imageElement.addAttribute(new Attribute("url", generateImageLocationFrom(item)));
        imageElement.addAttribute(new Attribute("width", "86"));
        imageElement.addAttribute(new Attribute("height", "48"));
        return imageElement;
    }

    private String generateImageLocationFrom(Item item) {
        Pattern p = Pattern.compile("(.*/)\\d+x\\d+(/.*).jpg");
        Matcher m = p.matcher(item.getImage());
        if (m.matches()) {
            return m.group(1) + "86x48" + m.group(2) + ".jpg";
        }
        
        p = Pattern.compile("(.*)_\\d+_\\d+.jpg");
        m = p.matcher(item.getImage());
        if (m.matches()) {
            return m.group(1) + "_86_48.jpg";
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