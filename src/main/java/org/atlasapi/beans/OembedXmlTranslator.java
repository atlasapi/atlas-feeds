package org.atlasapi.beans;

import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.OembedOutput;
import org.atlasapi.feeds.XmlOembedItem;

/**
 * {@link OembedTranslator} that creates an ouput in XML format.
 *  
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class OembedXmlTranslator extends OembedTranslator {

	public OembedXmlTranslator() {
		super(new XmlOutputFactory());
	}

	private static class XmlOutputFactory implements OutputFactory {

		public OembedOutput createOutput() {
			try {
				return new XmlOembedItem();
			} catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		}
		
	}

}
