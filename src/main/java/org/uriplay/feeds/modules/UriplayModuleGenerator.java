/* Copyright 2009 British Broadcasting Corporation
   Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.uriplay.feeds.modules;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.uriplay.media.TransportType;
import org.uriplay.media.entity.Encoding;
import org.uriplay.media.entity.Item;
import org.uriplay.media.entity.Location;
import org.uriplay.media.entity.Version;
import org.uriplay.media.reference.entity.MimeType;
import org.uriplay.media.vocabulary.MEDIA;
import org.uriplay.media.vocabulary.PLAY;
import org.uriplay.media.vocabulary.RDF;

import com.google.common.collect.Sets;
import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleGenerator;

/**
 * Rome generator for extension module to support URIplay custom tags and attributes.
 * Can manipulate item elements, and add attributes or child elements.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 * @author Lee Denison (lee@metabroadcast.com)
 */
public class UriplayModuleGenerator implements ModuleGenerator {

	private static final Namespace PLAY_NS = Namespace.getNamespace(PLAY.PREFIX, PLAY.NS);
	private static final Namespace MEDIA_NS = Namespace.getNamespace(MEDIA.PREFIX, MEDIA.NS);
	private static final Namespace RDF_NS = Namespace.getNamespace(RDF.PREFIX, RDF.NS);
	private static final Set<Namespace> NAMESPACES;
	
	static {
		Set<Namespace> namespaces = Sets.newHashSet();
		namespaces.add(PLAY_NS);
		namespaces.add(RDF_NS);
		namespaces.add(MEDIA_NS);
		NAMESPACES = Collections.unmodifiableSet(namespaces);
	}

	public String getNamespaceUri() {
		return PLAY.NS;
	}

	public Set<Namespace> getNamespaces() {
		return NAMESPACES;
	}

	public void generate(Module module, Element element) {
		
		UriplayModule uriplayModule = (UriplayModule) module;
		Object bean = uriplayModule.getBean();
	
		if (bean != null) {

			if (bean instanceof Item) {
				Item item = (Item) bean;

				String guidLink = null;
				String firstLocationUri = null;
				
				for (Version version : item.getVersions()) {
					for (Encoding encoding : version.getManifestedAs()) {
						for (Location location : encoding.getAvailableAt()) {
							
							if (location.getCanonicalUri() == null) {
								continue; // can't add enclosure with no location uri
							}
				
							if (firstLocationUri == null) {
								firstLocationUri = location.getCanonicalUri();
							}
							
							if (guidLink == null && TransportType.LINK.equals(location.getTransportType())) {
								guidLink = location.getCanonicalUri();
							}
							
							// FIXME: should only need to pass in element and location
							@SuppressWarnings("unused")
							Element enclosure = addEnclosureElement(element, version, encoding, location);
							Element content = addMediaContentElement(element, version, encoding, location);
							
							addChildElement(content, encoding.getAudioBitRate(), "audioBitRate", PLAY_NS);
							addChildElement(content, encoding.getVideoBitRate(), "videoBitRate", PLAY_NS);
						}
					}
				}
				
				if (guidLink == null && item.getCanonicalUri() != null) {
					guidLink = item.getCanonicalUri();
				}
				
				if (guidLink == null) {
					guidLink = firstLocationUri;
				}
				
				addGuidTo(element, guidLink);							
				addLinkTo(element, guidLink);
			}
		}
	}

	private void addLinkTo(Element element, String guidLink) {
		Element link = element.getChild("link");
		if (link == null) {
			Text linkTxt = new Text(guidLink);
			link = new Element("link");
			link.addContent(linkTxt);
			element.addContent(link);
		}
	}

	private void addGuidTo(Element element, String guidLink) {
		Element guid = element.getChild("guid");
		if (guid == null) {
			Text guidTxt = new Text(guidLink);
			guid = new Element("guid");
			guid.addContent(guidTxt);
			element.addContent(guid);
		}
	}

	private void addChildElement(Element element, Integer value, String attribute, Namespace namespace) {
		if (value != null) {
			Element childElm = new Element(attribute, namespace);
			Text childVal = new Text(value.toString());
			
			childElm.addContent(childVal);
			element.addContent(childElm);
		}
	}

	private Element addEnclosureElement(Element element, Version version, Encoding encoding, Location location) {
		Element enclosure = getEnclosureElement(element, location.getCanonicalUri());
		
		addAttribute(enclosure, "type", encoding.getDataContainerFormat());
		addAttribute(enclosure, "length", encoding.getDataSize());
		
		return enclosure;
	}

	private Element addMediaContentElement(Element element, Version version, Encoding encoding, Location location) {
		Element contentElm = new Element("content", MEDIA_NS);
		
		contentElm.setAttribute("url", location.getCanonicalUri());
		
		addDataContainerFormat(contentElm, encoding);
		addAttribute(contentElm, "fileSize", encoding.getDataSize());
		addAttribute(contentElm, "bitrate", encoding.getBitRate());
		addAttribute(contentElm, "framerate", encoding.getVideoFrameRate());
		addAttribute(contentElm, "channels", encoding.getAudioChannels());
		addAttribute(contentElm, "duration", version.getDuration());
		addAttribute(contentElm, "width", encoding.getVideoHorizontalSize());
		addAttribute(contentElm, "height", encoding.getVideoVerticalSize());
	
		Element mediaPlayerElm = new Element("player", MEDIA_NS);
		addAttribute(mediaPlayerElm, "url", location.getCanonicalUri());
		contentElm.addContent(mediaPlayerElm);
								
		element.addContent(contentElm);
		
		return contentElm;
	}

	private void addAttribute(Element contentElm, String attribute, Object value) {
		if (value != null) {
			contentElm.setAttribute(attribute, value.toString());
		}
	}

	private void addDataContainerFormat(Element contentElm, Encoding encoding) {
		MimeType containerFormat = encoding.getDataContainerFormat();
		if (containerFormat != null) {
			contentElm.setAttribute("type", encoding.getDataContainerFormat().toString());
			if (containerFormat.getParentType().equals("video")) {
				contentElm.setAttribute("medium", "video");
			} else if (containerFormat.getParentType().equals("video")) {
				contentElm.setAttribute("medium", "audio");
			}
		}
	}

	private Element getEnclosureElement(Element element, String uri) {
		Iterator<?> children = element.getContent().iterator(); 
		Element enclosure = null;

		while (children.hasNext() && enclosure == null) {
			Object child = children.next();
			
			if (child instanceof Element) {
				Element childElm = (Element) child;
				
				if (childElm.getAttribute("url") != null && childElm.getAttribute("url").equals(uri)) {
					enclosure = childElm;
				}
			}
		}
		
		if (enclosure == null) {
			enclosure = new Element("enclosure");
			enclosure.setAttribute("url", uri);
			element.addContent(enclosure);
		}
		
		return enclosure;
	}
}
