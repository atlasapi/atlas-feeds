package org.atlasapi.feeds.sitemaps;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Countries;
import org.atlasapi.media.entity.Country;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.text.Truncator;
import com.metabroadcast.common.url.UrlEncoding;

public class SiteMapOutputter {

	private static final DateTimeFormatter DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
	
	private static final XMLNamespace SITEMAP = new XMLNamespace("sitemap", "http://www.sitemaps.org/schemas/sitemap/0.9");
	private static final XMLNamespace VIDEO = new XMLNamespace("video", "http://www.google.com/schemas/sitemap-video/1.1");
	
	private static final Truncator descTruncator = new Truncator().onlyTruncateAtAWordBoundary().withMaxLength(2048);
	private static final Truncator titleTruncator = new Truncator().onlyTruncateAtAWordBoundary().withMaxLength(100);
	
	public void output(List<Item> feed, OutputStream out) throws IOException {
		Element feedElem = createFeed(feed);
	    write(out, feedElem);  
	}
	

	private Element createFeed(List<Item> items) {
		Element feed = new Element("urlset", SITEMAP.getUri());
		VIDEO.addDeclarationTo(feed);
		
		for (Item item : items) {
			Location location = locationFrom(item);
			if (location != null) {
				feed.appendChild(videoEntry(item, location));
			}
		}
		
		return feed;
	}


	private Element videoEntry(Item item, Location location) {
		Element urlElement = createElement("url", SITEMAP);
		urlElement.appendChild(stringElement("loc", SITEMAP, location.getUri()));
		if (location.getAvailable()) {
			urlElement.appendChild(videoElem(item, location));
		}
		return urlElement;
	}


	private Element videoElem(Item item, Location location) {
		Element videoElem = createElement("video", VIDEO);
		videoElem.appendChild(stringElement("thumbnail_loc", VIDEO, item.getThumbnail()));
		videoElem.appendChild(stringElement("title", VIDEO, titleTruncator.truncatePossibleNull(item.getTitle())));
		videoElem.appendChild(stringElement("description", VIDEO, descTruncator.truncatePossibleNull(item.getDescription())));
		
		Integer duration = getDuration(item);
		if (duration != null) {
			videoElem.appendChild(stringElement("duration", VIDEO, String.valueOf(duration)));
		}
		Policy policy = location.getPolicy();
		if (policy != null) {
			DateTime availableTill = policy.getAvailabilityEnd();
			if (availableTill != null) {
				videoElem.appendChild(stringElement("expiration_date", VIDEO, DATE_TIME_FORMAT.print(availableTill)));
			}
			Set<Country> countries = policy.getAvailableCountries();
			if (!countries.contains(Countries.ALL)) {
				String spaceDelimted = Joiner.on(' ').join(Iterables.transform(countries, Country.UNPACK_COUNTRY_CODE));
				Element restrictionElem = createElement("restriction", VIDEO);
				restrictionElem.addAttribute(new Attribute("relationship","allow"));
				restrictionElem.appendChild(spaceDelimted);
				videoElem.appendChild(restrictionElem);
			}

		}
		
		if (Publisher.C4.equals(item.getPublisher())) {
			c4playerLoc(videoElem, item, location);
		}
		
		return videoElem;
	}


	private void c4playerLoc(Element videoElem, Item item, Location location) {
		Element playerLocElem = createElement("player_loc", VIDEO);
		playerLocElem.addAttribute(new Attribute("allow_embed","false"));
		//http://www.channel4.com/static/programmes/asset/flash/swf/4odplayer-4.71.swf?brandTitle=Grand%20Designs&wsBrandTitle=grand-designs&primaryColor=0x0087E1&secondaryColor=0x0096FF&invertSkin=false&preSelectAsset=3121617&preSelectAssetGuidance=&preSelectAssetImageURL=/assets/programmes/images/grand-designs/series-7/9867d8e4-1c2e-422c-a99d-96257bd0e4ae_625x352.jpg&pinRequestCallback=C4.PinController.doPinChecks
		Brand brand = ((Episode)item).getBrand();
		String playerLoc = "http://www.channel4.com/static/programmes/asset/flash/swf/4odplayer-4.71.swf?brandTitle="+
							brand.getTitle()+ 
							"&wsBrandTitle="+
							brand.getCanonicalUri().substring(brand.getCanonicalUri().lastIndexOf('/')+1) +
							"&primaryColor=0x0087E1&secondaryColor=0x0096FF&invertSkin=false&preSelectAsset=" +
							location.getUri().substring(location.getUri().lastIndexOf('#')+1) + 
							"&preSelectAssetGuidance=&preSelectAssetImageURL="+
							item.getImage().replace("http://www.channel4.com", "") + 
							"&pinRequestCallback=C4.PinController.doPinChecks";
		playerLocElem.appendChild(playerLoc);
		videoElem.appendChild(playerLocElem);
	}

	private Integer getDuration(Item item) {
		for(Version version : item.getVersions()){
			return version.getDuration();
		}
		return null;
	}


	private Location locationFrom(Item item) {
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
