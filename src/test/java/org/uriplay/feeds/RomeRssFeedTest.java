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

package org.uriplay.feeds;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

import org.uriplay.feeds.SyndicationFeed.Item;

/**
 * Unit test for {@link RomeSyndicationFeed}.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class RomeRssFeedTest extends TestCase {
	
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	SyndicationFeed rssFeed;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		rssFeed = new RomeSyndicationFeed("rss_2.0");
		rssFeed.setTitle("test");
		rssFeed.setDescription("test description");
		rssFeed.setLink("http://example.com");
	}
	
	public void testCanBeWrittenToOutputStream() throws Exception {
		
		rssFeed.writeTo(outputStream);
		assertThat(outputStream.toString(), containsString("rss"));
	}
	
	public void testContainsEachItemAdded() throws Exception {
		
		Item item = rssFeed.createItem("http://sheep.com/maisy");
		item.setTitle("sheep");
		item.setDescription("a woolly animal");
		rssFeed.writeTo(outputStream);
		assertThat(outputStream.toString(), containsString("sheep"));
	}
	
}
