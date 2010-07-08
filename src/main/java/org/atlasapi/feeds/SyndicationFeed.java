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

package org.atlasapi.feeds;

import java.io.OutputStream;

import com.sun.syndication.feed.module.Module;

/**
 * Simple syndication feed abstraction, allowing feeds to be built up from items.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public interface SyndicationFeed {

	public interface Item {
		
		void setTitle(String title);

		void setDescription(String desc);

		Module getModule(String uri);

	}

	SyndicationFeed.Item createItem(String uri);

	void writeTo(OutputStream outputStream);

	void setTitle(String title);

	void setDescription(String desc);

	void setLink(String uri);
	
	Module getModule(String uri);

}
