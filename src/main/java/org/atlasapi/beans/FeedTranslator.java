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

package org.atlasapi.beans;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.atlasapi.feeds.SyndicationFeed;
import org.atlasapi.feeds.modules.AtlasFeedModule;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.vocabulary.PLAY_USE_IN_RDF_FOR_BACKWARD_COMPATIBILITY;
import org.springframework.web.util.HtmlUtils;

import com.google.common.collect.Lists;

/**
 * {@link DocumentTranslator} that renders URIplay objects as a feed, either atom or RSS.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class FeedTranslator implements BeanGraphWriter {
	
	interface FeedFactory {
		SyndicationFeed createFeed();
	}
	
	private final FeedFactory feedFactory;

	protected FeedTranslator(FeedFactory feedFactory) {
		this.feedFactory = feedFactory;
	}

	public void writeTo(Collection<Object> graph, OutputStream stream) {

		SyndicationFeed feed = feedFactory.createFeed();
		
		for (Object bean : graph) {
			
			if (bean instanceof Playlist) {
				
				Playlist playlist = (Playlist) bean;
				
				feed.setTitle(playlist.getTitle());
				feed.setDescription(playlist.getDescription());
				if (playlist.getDescription() == null || playlist.getDescription().trim().equals("")) {
					feed.setDescription("URIplay output");
				}
				feed.setLink(playlist.getCanonicalUri());
				
				if (feed.getModule(PLAY_USE_IN_RDF_FOR_BACKWARD_COMPATIBILITY.NS) != null
					&& feed.getModule(PLAY_USE_IN_RDF_FOR_BACKWARD_COMPATIBILITY.NS) instanceof AtlasFeedModule) {
					((AtlasFeedModule) feed.getModule(PLAY_USE_IN_RDF_FOR_BACKWARD_COMPATIBILITY.NS)).setBean(bean);
				}
				
				//FIXME: playlist items are stored in a Set, so inherently unordered, leads to unordered feeds.
				for (Item item : playlist.getItems()) {
					
					if (item.getCanonicalUri() == null) { continue; }
					
					org.atlasapi.feeds.SyndicationFeed.Item rssItem = feed.createItem(item.getCanonicalUri());
					rssItem.setTitle(item.getTitle());
					
					rssItem.setDescription(descriptionOf(item));

					if (rssItem.getModule(PLAY_USE_IN_RDF_FOR_BACKWARD_COMPATIBILITY.NS) != null
						&& rssItem.getModule(PLAY_USE_IN_RDF_FOR_BACKWARD_COMPATIBILITY.NS) instanceof AtlasFeedModule) {
						((AtlasFeedModule) rssItem.getModule(PLAY_USE_IN_RDF_FOR_BACKWARD_COMPATIBILITY.NS)).setBean(item);
					}
				}
			}
		}
		
		feed.writeTo(stream);
	}

	private String descriptionOf(Item item) {
		
		StringBuffer rssDescription = new StringBuffer();
		
		if (item.getDescription() != null) {
			rssDescription.append(item.getDescription());
		}
		
		String embedCode = HtmlUtils.htmlEscape(embedCodeFor(item));
		
		if (embedCode != null) {
			rssDescription.append(" ");
			rssDescription.append(embedCode);
		}
		
		return rssDescription.toString();
	}
	
	private String embedCodeFor(Item item) {
		List<Location> locations = locationsFor(item);
		for (Location location : locations) {
			if (location.getEmbedCode() != null) {
				return location.getEmbedCode();
			}
		}
		return null;
	}

	private List<Location> locationsFor(Item item) {
		List<Location> allLocations = Lists.newArrayList();
		for (Version version : item.getVersions()) {
			for (Encoding encoding : version.getManifestedAs()) {
				allLocations.addAll(encoding.getAvailableAt());
			}
		}
		return allLocations;
	}

	@Override
	public void writeError(AtlasErrorSummary exception, OutputStream oStream) {
		//no-op
	}
}
