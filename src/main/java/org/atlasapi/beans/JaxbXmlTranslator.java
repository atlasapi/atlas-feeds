/* Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.beans;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atlasapi.media.entity.simple.Broadcast;
import org.atlasapi.media.entity.simple.ContentQueryResult;
import org.atlasapi.media.entity.simple.Item;
import org.atlasapi.media.entity.simple.Location;
import org.atlasapi.media.entity.simple.Playlist;
import org.atlasapi.media.entity.simple.PublisherDetails;
import org.atlasapi.media.entity.simple.ScheduleChannel;
import org.atlasapi.media.entity.simple.ScheduleQueryResult;
import org.atlasapi.media.vocabulary.DC;
import org.atlasapi.media.vocabulary.PLAY_SIMPLE_XML;
import org.atlasapi.media.vocabulary.PO;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Outputs simple URIplay model in plain XML format using JAXB.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class JaxbXmlTranslator implements AtlasModelWriter {

	private static final UriplayNamespacePrefixMapper PREFIX_MAPPER = new UriplayNamespacePrefixMapper();

	private static final String NS_MAPPER = "com.sun.xml.bind.namespacePrefixMapper";
	
	private final Log log = LogFactory.getLog(getClass());
	private final JAXBContext context;

	public JaxbXmlTranslator() {
		try {
			context = JAXBContext.newInstance(ContentQueryResult.class, ScheduleQueryResult.class, ScheduleChannel.class, Playlist.class, Item.class, Location.class, Broadcast.class, PublisherDetails.class);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ContentQueryResult readFrom(String data) {
		Unmarshaller unmarshaller;
		try {
			unmarshaller = context.createUnmarshaller();
			return (ContentQueryResult) unmarshaller.unmarshal(new StringReader(data));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private final SimpleTimeLimiter limiter = new SimpleTimeLimiter();
	
	@Override
	public void writeTo(final HttpServletRequest request, final HttpServletResponse response, Collection<Object> graph, AtlasModelType type) throws IOException {
		final Object result = Iterables.getOnlyElement(graph);
		try {
			limiter.callWithTimeout(writeOut(response, result), 60, TimeUnit.SECONDS, true);
		} catch (IOException e) {
			throw e;
		} catch (UncheckedTimeoutException timeout) { 
			log.error("Timed out writing " + request.getRequestURI() + "?" + request.getQueryString());
			writeError(request, response, AtlasErrorSummary.forException(timeout));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Callable<Void> writeOut(final HttpServletResponse response, final Object result) {
		return new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				Marshaller m = context.createMarshaller();
				m.setProperty(NS_MAPPER, PREFIX_MAPPER);
				m.marshal(result, response.getOutputStream());
				return null;
			}
		};
	}
	
	private static final class UriplayNamespacePrefixMapper extends NamespacePrefixMapper {
		@Override
		public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
			if (PLAY_SIMPLE_XML.NS.equals(namespaceUri)) {
				return PLAY_SIMPLE_XML.PREFIX;
			} else if (PO.NS.equals(namespaceUri)) {
				return PO.PREFIX;
			} else if (DC.NS.equals(namespaceUri)) {
				return "dc";
			}
			return null;
		}

		@Override
		public String[] getPreDeclaredNamespaceUris() {
		    return new String[] { PLAY_SIMPLE_XML.NS , PO.NS, DC.NS};
		}
	}
	
	@Override
	public void writeError(HttpServletRequest request, HttpServletResponse response, AtlasErrorSummary exception) {
		try {
			write(response.getOutputStream(), xmlFrom(exception));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Element xmlFrom(AtlasErrorSummary exception) {
		Element error = new Element("error");
		error.appendChild(stringElement("message", exception.message()));
		error.appendChild(stringElement("code", exception.errorCode()));
		error.appendChild(stringElement("id", exception.id()));
		return error;
	}

	private void write(OutputStream out, Element xml) throws UnsupportedEncodingException, IOException {
		Serializer serializer = new Serializer(out, Charsets.UTF_8.toString());
		serializer.setIndent(4);
		serializer.setLineSeparator("\n");
		serializer.write(new Document(xml));
	}
	
	protected Element stringElement(String name, String value) {
		Element elem = new Element(name);
		elem.appendChild(value);
		return elem;
	} 
}
