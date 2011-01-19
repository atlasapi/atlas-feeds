package org.atlasapi.feeds.sitemaps;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;

import javax.servlet.ServletOutputStream;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Clip;
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
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.text.Truncator;

public class SiteMapOutputter {

	private static final DateTimeFormatter DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
	
	private static final XMLNamespace SITEMAP = new XMLNamespace("sitemap", "http://www.sitemaps.org/schemas/sitemap/0.9");
	private static final XMLNamespace VIDEO = new XMLNamespace("video", "http://www.google.com/schemas/sitemap-video/1.1");
	
	private static final Truncator descTruncator = new Truncator().onlyTruncateAtAWordBoundary().withMaxLength(2048);
	private static final Truncator titleTruncator = new Truncator().onlyTruncateAtAWordBoundary().withMaxLength(100);
	
	public void output(Iterable<Item> feed, OutputStream out) throws IOException {
		Element feedElem = createFeed(feed);
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

	private Element createFeed(Iterable<Item> items) {
		Element feed = new Element("urlset", SITEMAP.getUri());
		VIDEO.addDeclarationTo(feed);
		for (Item item : items) {
			entryForItem(feed, item);
			for (Clip clip : item.getClips()) {
				entryForItem(feed, clip);
			}
		}
		return feed;
	}

	private void entryForItem(Element feed, Item item) {
		Location location = locationFrom(item);
		if (location != null) {
			feed.appendChild(urlEntry(item, location));
		}
	}

	private Element urlEntry(Item item, Location location) {
		Element urlElement = createElement("url", SITEMAP);
		urlElement.appendChild(stringElement("loc", SITEMAP, location.getUri()));
		if (item.getLastUpdated() != null) {
			urlElement.appendChild(stringElement("lastmod", SITEMAP, DATE_TIME_FORMAT.print(item.getLastUpdated())));
		}
		if (location.getAvailable()) {
			urlElement.appendChild(videoElem(item, location));
		}
		return urlElement;
	}

	private Element videoElem(Item item, Location location) {
		Element videoElem = createElement("video", VIDEO);
		videoElem.appendChild(stringElement("thumbnail_loc", VIDEO, item.getThumbnail()));
		videoElem.appendChild(stringElement("title", VIDEO, itemTitle(item)));
		videoElem.appendChild(stringElement("description", VIDEO, descTruncator.truncatePossibleNull(item.getDescription())));
		
		Integer duration = getDuration(item);
		if (duration != null) {
			videoElem.appendChild(stringElement("duration", VIDEO, String.valueOf(duration)));
		}
		
//		Unmaintainable / not needed.
//		if (Publisher.C4.equals(item.getPublisher())) {
//			if (item instanceof Episode) {
//				c4playerLoc(videoElem, item, location);
//			}
//		}
		
		//TODO: family friendly?
		
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
		return videoElem;
	}

	private String itemTitle(Item item) {
		String title = Strings.nullToEmpty(item.getTitle());
		if (item instanceof Episode) {
			Brand brand = ((Episode) item).getContainer();
			if (brand != null && !Strings.isNullOrEmpty(brand.getTitle())) {
				String brandTitle = brand.getTitle();
				if (!brandTitle.equals(title)) {
					title = brandTitle + " : " + title;
				}
			}
		}
		return titleTruncator.truncate(title);
	}


	private void c4playerLoc(Element videoElem, Item item, Location location) {
		Element playerLocElem = createElement("player_loc", VIDEO);
		playerLocElem.addAttribute(new Attribute("allow_embed","no"));
		Brand brand = ((Episode)item).getContainer();
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
		for(Version version : item.nativeVersions()){
			return version.getDuration();
		}
		return null;
	}


	private Location locationFrom(Item item) {
		for (Version version : item.nativeVersions()) {
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
