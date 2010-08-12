package org.atlasapi.feeds.interlinking.outputting;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Namespace;
import nu.xom.Serializer;

import org.atlasapi.feeds.interlinking.InterlinkBase;
import org.atlasapi.feeds.interlinking.InterlinkBrand;
import org.atlasapi.feeds.interlinking.InterlinkBroadcast;
import org.atlasapi.feeds.interlinking.InterlinkContent;
import org.atlasapi.feeds.interlinking.InterlinkEpisode;
import org.atlasapi.feeds.interlinking.InterlinkFeed;
import org.atlasapi.feeds.interlinking.InterlinkOnDemand;
import org.atlasapi.feeds.interlinking.InterlinkSeries;
import org.atlasapi.feeds.interlinking.InterlinkFeed.InterlinkFeedAuthor;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;

import com.google.common.base.Charsets;

public class InterlinkFeedOutputter {

	private static final DateTimeFormatter DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
	
	private static final XMLNamespace NS_ATOM = new XMLNamespace("atom", "http://www.w3.org/2005/Atom");
	private static final XMLNamespace NS_DC = new XMLNamespace("dc", "http://purl.org/dc/terms");
	private static final XMLNamespace NS_ILINK = new XMLNamespace("ilink", "http://www.bbc.co.uk/developer/interlinking");
	private static final XMLNamespace NS_MRSS = new XMLNamespace("media", "http://search.yahoo.com/mrss/");
	
	public void output(InterlinkFeed feed, OutputStream out) throws IOException {
		Element feedElem = createFeed(feed);
		for (InterlinkBrand brand : feed.brands()) {
			feedElem.appendChild(brandToEntry(brand));

			for (InterlinkEpisode episode : brand.episodesWithoutASeries()) {
				appendEpisodeTo(feedElem, episode, brand);
			}
			
			for (InterlinkSeries series : brand.series()) {
				
				feedElem.appendChild(seriesToEntry(series, brand));

				for (InterlinkEpisode episode : series.episodes()) {
					appendEpisodeTo(feedElem, episode, series);
				}
			}
		}
	    write(out, feedElem);  
	}

	private void appendEpisodeTo(Element feedElem,  InterlinkEpisode episode, InterlinkContent context) {
		feedElem.appendChild(episodeToEntry(episode, context));
		
		for (InterlinkBroadcast broadcast : episode.broadcasts()) {
			feedElem.appendChild(broadcastToEntry(broadcast, episode));
		}
		for (InterlinkOnDemand onDemand : episode.onDemands()) {
			feedElem.appendChild(onDemandToEntry(onDemand, episode));
		}
	}
	
	private Element onDemandToEntry(InterlinkOnDemand onDemand, InterlinkEpisode parent) {
		Element entry = createElement("entry", NS_ATOM);
		entry.appendChild(stringElement("id", NS_ATOM, onDemand.id()));
		entry.appendChild(stringElement("type", NS_ILINK, "ondemand"));
		if (onDemand.lastUpdated() != null) {
            entry.appendChild(stringElement("updated", NS_ATOM, onDemand.lastUpdated().toString(DATE_TIME_FORMAT)));
        }
		Element mrssContent = createElement("content", NS_MRSS);
		mrssContent.appendChild(stringElement("parent_id", NS_ILINK, parent.id()));
		mrssContent.appendChild(stringElement("availability_start", NS_ILINK, onDemand.availabilityStart().toString(DATE_TIME_FORMAT)));
		mrssContent.appendChild(stringElement("availability_end", NS_ILINK, onDemand.availabilityEnd().toString(DATE_TIME_FORMAT)));
		mrssContent.appendChild(stringElement("duration", NS_ILINK, duration(onDemand.availabilityStart(), onDemand.availabilityEnd())));
		
		// TODO: Static attributes for now
		mrssContent.appendChild(stringElement("platform_code", NS_ILINK, "pc"));
		mrssContent.appendChild(stringElement("payment_type", NS_ILINK, "free"));
		entry.appendChild(atomContentElementContaining(mrssContent));
		return entry;
	}
	
	private String duration(DateTime start, DateTime end) {
	    Period period = new Period(start, end);
	    StringBuffer duration = new StringBuffer("PT");
	    
	    int hours = period.getHours();
	    int minutes = period.getMinutes();
	    int seconds = period.getSeconds();
	    
	    if (hours > 0) {
	        duration.append(hours+"H");
	    }
	    if (minutes > 0 || hours > 0) {
	        duration.append(minutes+"M");
	    }
	    duration.append(seconds+"S");
	    
	    return duration.toString();
	}

	private Element broadcastToEntry(InterlinkBroadcast broadcast, InterlinkEpisode parent) {
		Element entry = createElement("entry", NS_ATOM);
		entry.appendChild(stringElement("id", NS_ATOM, broadcast.id()));
		entry.appendChild(stringElement("type", NS_ILINK, "broadcast"));
		if (broadcast.lastUpdated() != null) {
            entry.appendChild(stringElement("updated", NS_ATOM, broadcast.lastUpdated().toString(DATE_TIME_FORMAT)));
        }
		Element mrssContent = createElement("content", NS_MRSS);
		mrssContent.appendChild(stringElement("parent_id", NS_ILINK, parent.id()));
		
		DateTime broadcastStart = broadcast.broadcastStart();
		if (broadcastStart != null) {
			mrssContent.appendChild(stringElement("broadcast_start", NS_ILINK, broadcastStart.toString(DATE_TIME_FORMAT)));
		}
		
		if (broadcast.duration() != null) {
			mrssContent.appendChild(stringElement("duration", NS_ILINK, ISOPeriodFormat.standard().print(broadcast.duration().toPeriod())));
		}
		entry.appendChild(atomContentElementContaining(mrssContent));
		return entry;
	}

	private Element episodeToEntry(InterlinkEpisode episode, InterlinkContent parent) {
		Element entry = createElement("entry", NS_ATOM);
		addCommonFieldsTo(episode, entry);
		entry.appendChild(stringElement("type", NS_ILINK, "episode"));
		
		Element linkElement = createElement("link", NS_ATOM);
		linkElement.addAttribute(new Attribute("href", episode.link()));
		linkElement.addAttribute(new Attribute("rel", "alternate"));
		entry.appendChild(linkElement);
		
		addCommonContentFieldsTo(episode, entry, parent);
		return entry;
	}

	private Element seriesToEntry(InterlinkSeries series, InterlinkContent parent) {
		Element entry = createElement("entry", NS_ATOM);
		addCommonFieldsTo(series, entry);
		entry.appendChild(stringElement("type", NS_ILINK, "series"));
		addCommonContentFieldsTo(series, entry, parent);
		return entry;
	}
	
	private Element brandToEntry(InterlinkBrand brand) {
		Element entry = createElement("entry", NS_ATOM);
		addCommonFieldsTo(brand, entry);
		entry.appendChild(stringElement("type", NS_ILINK, "brand"));
		addCommonContentFieldsTo(brand, entry, null);
		return entry;
	}

	private Element contentElement(InterlinkContent content, InterlinkContent parent) {
		Element mrssContent = createElement("content", NS_MRSS);
		
		if (content.description() != null) {
			mrssContent.appendChild(stringElement("description", NS_MRSS, content.description()));
		}
		if (parent != null) {
			mrssContent.appendChild(stringElement("parent_id", NS_ILINK, parent.id()));
			mrssContent.appendChild(stringElement("index", NS_ILINK, String.valueOf(content.indexWithinParent())));
		}
		return atomContentElementContaining(mrssContent);
	}

	private Element atomContentElementContaining(Element element) {
		Element atomContent = createElement("content", NS_ATOM);
		atomContent.addAttribute(new Attribute("type", "application/xml"));
		atomContent.appendChild(element);
		return atomContent;
	}



	private void addCommonContentFieldsTo(InterlinkContent content, Element entry, InterlinkContent parent) {
		if (content.summary() != null) {
			entry.appendChild(stringElement("summary", NS_ATOM, content.summary()));
		}
		if (content.lastUpdated() != null) {
		    entry.appendChild(stringElement("updated", NS_ATOM, content.lastUpdated().toString(DATE_TIME_FORMAT)));
		}
		entry.appendChild(contentElement(content, parent));

	}

	private Element createFeed(InterlinkFeed feed) {
		Element feedElem = new Element("feed", NS_ATOM.uri);

		NS_DC.addDeclarationTo(feedElem);
		NS_MRSS.addDeclarationTo(feedElem);
		NS_ILINK.addDeclarationTo(feedElem);
		
		feedElem.addAttribute(new Attribute("xml:lang", Namespace.XML_NAMESPACE, "en-GB"));
		
		feedElem.appendChild(stringElement("title", NS_ATOM, feed.title()));
		feedElem.appendChild(stringElement("subtitle", NS_ATOM, feed.subtitle()));
		feedElem.appendChild(stringElement("updated", NS_ATOM, feed.updated().toString(DATE_TIME_FORMAT)));
		feedElem.appendChild(stringElement("id", NS_ATOM, feed.id()));

		Element authorElem = createElement("author", NS_ATOM);
		InterlinkFeedAuthor author = feed.author();
		authorElem.appendChild(stringElement("partner", NS_ILINK, author.partner()));
		authorElem.appendChild(stringElement("supplier", NS_ILINK, author.supplier()));
		feedElem.appendChild(authorElem);
		
		return feedElem;
	}
	
	private void addCommonFieldsTo(InterlinkBase base, Element entry) {
		entry.appendChild(stringElement("title", NS_ATOM, base.title()));
		entry.appendChild(stringElement("id", NS_ATOM, base.id()));
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
		Element elem = new Element(name, ns.uri);
		if (!NS_ATOM.equals(ns)) {
			elem.setNamespacePrefix(ns.prefix);
		}
		return elem;
	}
	
	private final static class XMLNamespace {
		
		private final String uri;
		private final String prefix;

		public XMLNamespace(String prefix,String uri) {
			this.prefix = prefix;
			this.uri = uri;
		}
		
		void addDeclarationTo(Element elem) {
			elem.addNamespaceDeclaration(prefix, uri);
		}
	}
}
