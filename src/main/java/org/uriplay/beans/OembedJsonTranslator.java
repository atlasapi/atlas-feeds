package org.uriplay.beans;

import org.uriplay.feeds.JsonOembedItem;
import org.uriplay.feeds.OembedOutput;

/**
 * {@link OembedTranslator} that creates an ouput in Json format.
 *  
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class OembedJsonTranslator extends OembedTranslator {

	public OembedJsonTranslator() {
		super(new JsonOutputFactory());
	}

	private static class JsonOutputFactory implements OutputFactory {

		public OembedOutput createOutput() {
			return new JsonOembedItem();
		}
		
	}

}
