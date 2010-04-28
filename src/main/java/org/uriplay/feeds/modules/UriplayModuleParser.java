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

import org.jdom.Element;
import org.uriplay.media.vocabulary.PLAY;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleParser;

/**
 * We currently cannot parse our custom tags, only generate.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class UriplayModuleParser implements ModuleParser {

	public String getNamespaceUri() {
		return PLAY.NS;
	}

	public Module parse(Element element) {
		throw new UnsupportedOperationException();
	}

}
