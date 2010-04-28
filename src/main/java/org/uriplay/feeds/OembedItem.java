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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Robert Chatley (robert@metabroadcast.com)
 */
@XmlRootElement(name="oembed")
public class OembedItem {

	protected static final char QUOTE = '"';
	protected String type;
	
	@XmlElement(name="provider_url")
	protected String providerUrl;
	@XmlElement(name="title")
	protected String title;
	@XmlElement(name="width")
	protected int width;
	@XmlElement(name="height")
	protected int height;
	@XmlElement(name="thumbnail_url")
	protected String thumbnailUrl;
	
	@XmlElement(name="html")
	protected String embedCode;

	public OembedItem() {
		super();
	}

	public void setProviderUrl(String providerUrl) {
		this.providerUrl = providerUrl;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setEmbedCode(String embedCode) {
		this.embedCode = embedCode;
	}

	public String title() {
		return title;
	}

	public int height() {
		return height;
	}
	
	public int width() {
		return width;
	}

	public String providerUrl() {
		return providerUrl;
	}

	public String thumbnailUrl() {
		return thumbnailUrl;
	}
	
	public String embedCode() {
		return embedCode;
	}

}