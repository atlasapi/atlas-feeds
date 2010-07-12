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

package org.atlasapi.feeds.modules;

import org.atlasapi.media.vocabulary.PLAY;

import com.sun.syndication.feed.module.ModuleImpl;

/**
 * Rome extension module to support URIplay custom tags and attributes.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 * @author Lee Denison (lee@metabroadcast.com)
 */
public class RomeUriplayModule extends ModuleImpl implements AtlasFeedModule {

	private static final long serialVersionUID = 1L;

	private Object bean;
	
	public RomeUriplayModule() {
		super(AtlasFeedModule.class, PLAY.NS);
	}

	public Class<?> getInterface() {
		return AtlasFeedModule.class;
	}

	public void copyFrom(Object obj) {
		AtlasFeedModule module = (AtlasFeedModule) obj;
		setBean(module.getBean());
	}
	
	public Object clone() {
		RomeUriplayModule clone = new RomeUriplayModule();
		
		// FIXME: this doesn't clone bean!  Not sure why 
		// FIXME  cloning is necessary, will find out when this breaks.
		clone.setBean(bean);
		
		return clone;
	}

	public Object getBean() {
		return bean;
	}
	
	public void setBean(Object bean) {
		this.bean = bean;
	}
	
}
