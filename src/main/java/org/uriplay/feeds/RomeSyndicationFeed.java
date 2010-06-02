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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;

import org.uriplay.feeds.modules.RomeUriplayModule;

import com.google.common.collect.Lists;
import com.sun.syndication.feed.module.Module;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * RSS feed implementation using the ROME library.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class RomeSyndicationFeed implements SyndicationFeed {

	private final String feedType;
	
	@SuppressWarnings("unchecked")
    public RomeSyndicationFeed(String feedType) {
		this.feedType = feedType;

		this.feed.getModules().add(new RomeUriplayModule());
	}

	List<SyndEntry> entries = Lists.newArrayList();
	SyndFeed feed = new SyndFeedImpl();

	public void writeTo(OutputStream outputStream) {
		Writer writer;
		try {
			writer = new OutputStreamWriter(outputStream, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException(uee);
		}
		SyndFeedOutput output = new SyndFeedOutput();
		feed.setFeedType(feedType);
		feed.setEntries(entries);

		try {
			output.output(feed, writer);
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public SyndicationFeed.Item createItem(String uri) {		
		FeedItem item = new FeedItem(uri);
		entries.add(item.entry);
		return item;
	}

	public void setTitle(String title) {
		feed.setTitle(title);
	}

	public void setDescription(String desc) {
		feed.setDescription(desc);
	}	
	
	public void setLink(String uri) {
		feed.setLink(uri);
	}
	
	public Module getModule(String uri) {
		return feed.getModule(uri);
	}

	static class FeedItem implements SyndicationFeed.Item {

		SyndEntry entry;
		
		@SuppressWarnings("unchecked")
		public FeedItem(String uri) {
			entry = new SyndEntryImpl();			
			entry.getModules().add(new RomeUriplayModule());
		}
		
		public void setDescription(String desc) {
			SyndContent description = new SyndContentImpl();
			
			description.setType("text/plain");
			description.setValue(desc);
			entry.setDescription(description);
		}

		public void setTitle(String title) {
			entry.setTitle(title);
		}

		public Module getModule(String uri) {
			return entry.getModule(uri);
		}

	}

}
