package org.atlasapi.feeds.sitemaps;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.atlasapi.feeds.xml.XMLNamespace;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Charsets;

public class SiteMapIndexOutputter {

	private static final DateTimeFormatter DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
	
	private static final XMLNamespace SITEMAP = new XMLNamespace("sitemap", "http://www.sitemaps.org/schemas/sitemap/0.9");
	
	public void output(Iterable<SiteMapRef> refs, OutputStream out) throws IOException {
	    write(out, createFeed(refs));  
	}

	private Element createFeed(Iterable<SiteMapRef> refs) {
		Element feed = new Element("sitemapindex", SITEMAP.getUri());
		for (SiteMapRef ref : refs) {
			Element siteMapRefElem = createElement("sitemap", SITEMAP);
			siteMapRefElem.appendChild(stringElement("loc", SITEMAP, ref.getUrl()));
			feed.appendChild(siteMapRefElem);
		}
		return feed;
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
