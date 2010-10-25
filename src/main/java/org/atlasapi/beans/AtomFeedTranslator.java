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

import org.atlasapi.feeds.RomeSyndicationFeed;
import org.atlasapi.feeds.SyndicationFeed;

/**
 * Specialisation of {@link FeedTranslator} that outputs Atom 1.0
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class AtomFeedTranslator extends FeedTranslator {

	static class RomeRssFeedFactory implements FeedFactory {

		public SyndicationFeed createFeed() {
			return new RomeSyndicationFeed("atom_1.0");
		}
	}
	
	public AtomFeedTranslator() {
		super(new RomeRssFeedFactory());
	}
}
