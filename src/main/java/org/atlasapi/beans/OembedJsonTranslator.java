package org.atlasapi.beans;

import org.atlasapi.feeds.JsonOembedItem;
import org.atlasapi.feeds.OembedOutput;

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
