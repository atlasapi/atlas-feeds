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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 * Writes oEmbed representation as XML, using JAXB marshaller.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class XmlOembedItem extends OembedItem implements OembedOutput {

	private final JAXBContext context;
	
	public XmlOembedItem() throws JAXBException {
		context = JAXBContext.newInstance(OembedItem.class);
	}

	public void writeTo(OutputStream outputStream) {
		try {
		Marshaller m = context.createMarshaller();
		m.marshal(this, outputStream);
	
			outputStream.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
