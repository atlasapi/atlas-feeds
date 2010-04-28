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

package org.uriplay.beans;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.uriplay.beans.FeedTranslator.FeedFactory;
import org.uriplay.feeds.SyndicationFeed;
import org.uriplay.feeds.modules.UriplayModule;
import org.uriplay.media.entity.Encoding;
import org.uriplay.media.entity.Episode;
import org.uriplay.media.entity.Location;
import org.uriplay.media.entity.Playlist;
import org.uriplay.media.entity.Version;
import org.uriplay.media.vocabulary.PLAY;

import com.google.common.collect.Sets;

/**
 * Unit test for {@link FeedTranslator}.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class FeedTranslatorTest extends MockObjectTestCase {
	
	static final long DATA_SIZE = 10L;
	
	Set<Object> graph = Sets.<Object>newHashSet();
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	
	FeedFactory feedFactory = mock(FeedFactory.class);
	SyndicationFeed rssFeed = mock(SyndicationFeed.class);
	SyndicationFeed.Item item = mock(SyndicationFeed.Item.class);
	UriplayModule uriplayModule = mock(UriplayModule.class);
	
	public void testCreatesFeed() throws Exception {
		
		checking(new Expectations() {{ 
			one(feedFactory).createFeed(); 
		}});
		
		new FeedTranslator(feedFactory).writeTo(graph, outputStream);
	}
	
	public void testWritesFeedToStream() throws Exception {
		
		checking(new Expectations() {{ 
			allowing(feedFactory).createFeed(); will(returnValue(rssFeed));
			one(rssFeed).writeTo(outputStream);
		}});
		
		new FeedTranslator(feedFactory).writeTo(graph, outputStream);
	}
	
	public void testSetsFeedTitleAndDescriptionFromPlaylists() throws Exception {
		
		final Playlist playlist = new Playlist();
		playlist.setTitle("Test Title");
		playlist.setDescription("Test Description");
		playlist.setCanonicalUri("http://example.com");
		graph.add(playlist);
		
		checking(new Expectations() {{ 
			allowing(feedFactory).createFeed(); will(returnValue(rssFeed));
			allowing(rssFeed).writeTo(outputStream);
			allowing(rssFeed).getModule(PLAY.NS); will(returnValue(uriplayModule));
	
			one(rssFeed).setTitle("Test Title");
			one(rssFeed).setDescription("Test Description");
			one(rssFeed).setLink("http://example.com");
			one(uriplayModule).setBean(playlist);
		}});
		
		new FeedTranslator(feedFactory).writeTo(graph, outputStream);
	}
	
	public void testAddsItemForEachItemInPlaylist() throws Exception {
		
		final Playlist playlist = new Playlist();
		playlist.setTitle("Test Title");
		playlist.setDescription("Test Description");
		playlist.setCanonicalUri("http://example.com");
		
		final Episode episode = new Episode();
		episode.setTitle("Dr Who Ep 1");
		episode.setDescription("Timelord stuff");
		episode.setCanonicalUri("http://uriplay.org/dw");
		addLocationTo(episode, "http://example.com/dr-who.mp4");
		playlist.addItem(episode);
		
		graph.add(playlist);
		graph.add(episode);
		
		checking(new Expectations() {{ 
			allowing(feedFactory).createFeed(); will(returnValue(rssFeed));
			allowing(rssFeed).writeTo(outputStream);
			allowing(rssFeed).getModule(PLAY.NS); will(returnValue(uriplayModule));
			allowing(item).getModule(PLAY.NS); will(returnValue(uriplayModule));
	
			one(rssFeed).setTitle("Test Title");
			one(rssFeed).setDescription("Test Description");
			one(rssFeed).setLink("http://example.com");
			one(uriplayModule).setBean(playlist);
			
			one(rssFeed).createItem("http://uriplay.org/dw"); will(returnValue(item));
			one(item).setTitle("Dr Who Ep 1");
			one(item).setDescription("Timelord stuff");
			one(uriplayModule).setBean(episode);
		}});
		
		new FeedTranslator(feedFactory).writeTo(graph, outputStream);
	}

	private void addLocationTo(Episode episode, String locationUri) {
		Version version = new Version();
		Encoding encoding = new Encoding();
		encoding.setDataSize(DATA_SIZE);
		encoding.setAudioCoding("audio/mpeg");
		Location location = new Location();
		location.setCanonicalUri(locationUri);
		encoding.setAvailableAt(Sets.newHashSet(location));
		version.setManifestedAs(Sets.newHashSet(encoding));
		episode.setVersions(Sets.newHashSet(version));
	}

}
