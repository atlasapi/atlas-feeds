package org.atlasapi.feeds.lakeview.validation;

import org.joda.time.DateTime;

public class AzureFileAndMetadata {

	private byte[] contents;
	private DateTime lastModTime;
	
	public AzureFileAndMetadata(byte[] contents, DateTime lastModTime) {
		this.contents = contents;
		this.lastModTime = lastModTime;
	}

	public byte[] getContents() {
		return contents;
	}

	public DateTime getLastModTime() {
		return lastModTime;
	}

}
