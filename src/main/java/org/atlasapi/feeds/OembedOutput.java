package org.atlasapi.feeds;

import java.io.OutputStream;

/**
 * Construction interface for an object representing oEmbed data.
 *  
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public interface OembedOutput {

	void writeTo(OutputStream outputStream);

	void setTitle(String title);

	void setProviderUrl(String providerUrl);

	void setWidth(int width);

	void setHeight(int height);

	void setType(String type);

	void setEmbedCode(String embedCode);
}
