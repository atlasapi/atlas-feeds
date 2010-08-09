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
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;

import com.google.common.base.Charsets;

public class InterlinkFeedOutputter {

	private static final DateTimeFormatter DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
	
	private static final XMLNamespace NS_ATOM = new XMLNamespace("atom", "http://www.w3.org/2005/Atom");
	private static final XMLNamespace NS_DC = new XMLNamespace("dc", "http://purl.org/dc/terms");
	private static final XMLNamespace NS_ILINK = new XMLNamespace("ilink", "http://www.bbc.co.uk/interlinking");
	private static final XMLNamespace NS_MRSS = new XMLNamespace("media", "http://search.yahoo.com/mrss/");
	
	public void output(InterlinkFeed feed, OutputStream out) throws IOException {
		Element feedElem = createFeed(feed);
		for (InterlinkBrand brand : feed.brands()) {
			feedElem.appendChild(brandToEntry(brand));

			for (InterlinkSeries series : brand.series()) {
				feedElem.appendChild(seriesToEntry(series, brand));

				for (InterlinkEpisode episode : series.episodes()) {
					feedElem.appendChild(episodeToEntry(episode, series));
					
					for (InterlinkBroadcast broadcast : episode.broadcasts()) {
						feedElem.appendChild(broadcastToEntry(broadcast, episode));
					}
					for (InterlinkOnDemand onDemand : episode.onDemands()) {
						feedElem.appendChild(onDemandToEntry(onDemand, episode));
					}
				}
			}
		}
	    write(out, feedElem);  
	}
	
	private Element onDemandToEntry(InterlinkOnDemand onDemand, InterlinkEpisode parent) {
		Element entry = createElement("entry", NS_ATOM);
		entry.appendChild(stringElement("id", NS_ATOM, onDemand.id()));
		entry.appendChild(stringElement("type", NS_ILINK, "ondemand"));
		Element mrssContent = createElement("content", NS_MRSS);
		mrssContent.appendChild(stringElement("parent_id", NS_ILINK, parent.id()));
		entry.appendChild(atomContentElementContaining(mrssContent));
		return entry;
	}

	private Element broadcastToEntry(InterlinkBroadcast broadcast, InterlinkEpisode parent) {
		Element entry = createElement("entry", NS_ATOM);
		entry.appendChild(stringElement("id", NS_ATOM, broadcast.id()));
		entry.appendChild(stringElement("type", NS_ILINK, "broadcast"));
		Element mrssContent = createElement("content", NS_MRSS);
		mrssContent.appendChild(stringElement("parent_id", NS_ILINK, parent.id()));
		mrssContent.appendChild(stringElement("broadcast_start", NS_ILINK, broadcast.broadcastStart().toString(DATE_TIME_FORMAT)));
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
		entry.appendChild(contentElement(episode, parent));
		return entry;
	}

	private Element seriesToEntry(InterlinkSeries series, InterlinkContent parent) {
		Element entry = createElement("entry", NS_ATOM);
		addCommonFieldsTo(series, entry);
		entry.appendChild(stringElement("type", NS_ILINK, "series"));
		entry.appendChild(contentElement(series, parent));
		return entry;
	}

	private Element contentElement(InterlinkContent content, InterlinkContent parent) {
		Element mrssContent = createElement("content", NS_MRSS);
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

	private Element brandToEntry(InterlinkBrand brand) {
		Element entry = createElement("entry", NS_ATOM);
		addCommonFieldsTo(brand, entry);
		entry.appendChild(stringElement("type", NS_ILINK, "brand"));
		return entry;
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
