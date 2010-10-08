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

import java.io.ByteArrayOutputStream;
import java.util.Set;

import org.atlasapi.beans.FeedTranslator.FeedFactory;
import org.atlasapi.feeds.SyndicationFeed;
import org.atlasapi.feeds.modules.AtlasFeedModule;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.vocabulary.PLAY;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.Sets;
import com.metabroadcast.common.media.MimeType;

/**
 * Unit test for {@link FeedTranslator}.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
@RunWith(JMock.class)
public class FeedTranslatorTest  {
	
	private static final long DATA_SIZE = 10L;
	
	private final Mockery context = new Mockery();
	
	Set<Object> graph = Sets.<Object>newHashSet();
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	
	FeedFactory feedFactory = context.mock(FeedFactory.class);
	SyndicationFeed rssFeed = context.mock(SyndicationFeed.class);
	SyndicationFeed.Item item = context.mock(SyndicationFeed.Item.class);
	AtlasFeedModule atlasModule = context.mock(AtlasFeedModule.class);
	
	@Test
	public void testCreatesFeed() throws Exception {
		
		context.checking(new Expectations() {{ 
			one(feedFactory).createFeed(); 
		}});
		
		new FeedTranslator(feedFactory).writeTo(graph, outputStream);
	}
	
	@Test
	public void testWritesFeedToStream() throws Exception {
		
		context.checking(new Expectations() {{ 
			allowing(feedFactory).createFeed(); will(returnValue(rssFeed));
			one(rssFeed).writeTo(outputStream);
		}});
		
		new FeedTranslator(feedFactory).writeTo(graph, outputStream);
	}
	
	@Test
	public void testSetsFeedTitleAndDescriptionFromPlaylists() throws Exception {
		
		final Playlist playlist = new Playlist();
		playlist.setTitle("Test Title");
		playlist.setDescription("Test Description");
		playlist.setCanonicalUri("http://example.com");
		graph.add(playlist);
		
		context.checking(new Expectations() {{ 
			allowing(feedFactory).createFeed(); will(returnValue(rssFeed));
			allowing(rssFeed).writeTo(outputStream);
			allowing(rssFeed).getModule(PLAY.NS); will(returnValue(atlasModule));
	
			one(rssFeed).setTitle("Test Title");
			one(rssFeed).setDescription("Test Description");
			one(rssFeed).setLink("http://example.com");
			one(atlasModule).setBean(playlist);
		}});
		
		new FeedTranslator(feedFactory).writeTo(graph, outputStream);
	}
	
	@Test
	public void testAddsItemForEachItemInPlaylist() throws Exception {
		
		final Playlist playlist = new Playlist();
		playlist.setTitle("Test Title");
		playlist.setDescription("Test Description");
		playlist.setCanonicalUri("http://example.com");
		
		final Episode episode = new Episode();
		episode.setTitle("Dr Who Ep 1");
		episode.setDescription("Timelord stuff");
		episode.setCanonicalUri("http://atlasapi.org/dw");
		addLocationTo(episode, "http://example.com/dr-who.mp4");
		playlist.addItem(episode);
		
		graph.add(playlist);
		graph.add(episode);
		
		context.checking(new Expectations() {{ 
			allowing(feedFactory).createFeed(); will(returnValue(rssFeed));
			allowing(rssFeed).writeTo(outputStream);
			allowing(rssFeed).getModule(PLAY.NS); will(returnValue(atlasModule));
			allowing(item).getModule(PLAY.NS); will(returnValue(atlasModule));
	
			one(rssFeed).setTitle("Test Title");
			one(rssFeed).setDescription("Test Description");
			one(rssFeed).setLink("http://example.com");
			one(atlasModule).setBean(playlist);
			
			one(rssFeed).createItem("http://atlasapi.org/dw"); will(returnValue(item));
			one(item).setTitle("Dr Who Ep 1");
			one(item).setDescription("Timelord stuff");
			one(atlasModule).setBean(episode);
		}});
		
		new FeedTranslator(feedFactory).writeTo(graph, outputStream);
	}

	private void addLocationTo(Episode episode, String locationUri) {
		Version version = new Version();
		Encoding encoding = new Encoding();
		encoding.setDataSize(DATA_SIZE);
		encoding.setAudioCoding(MimeType.AUDIO_MPEG);
		Location location = new Location();
		location.setCanonicalUri(locationUri);
		encoding.setAvailableAt(Sets.newHashSet(location));
		version.setManifestedAs(Sets.newHashSet(encoding));
		episode.setVersions(Sets.newHashSet(version));
	}

}
