package org.atlasapi.feeds.upload.azure;

import java.net.ConnectException;
import java.net.URI;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploader;
import org.soyatec.windowsazure.blob.BlobStorageClient;
import org.soyatec.windowsazure.blob.IBlobContainer;
import org.soyatec.windowsazure.blob.IBlobContents;
import org.soyatec.windowsazure.blob.IBlobProperties;
import org.soyatec.windowsazure.blob.internal.BlobContents;
import org.soyatec.windowsazure.blob.internal.BlobProperties;
import org.soyatec.windowsazure.blob.internal.RetryPolicies;
import org.soyatec.windowsazure.blob.io.BlobMemoryStream;
import org.soyatec.windowsazure.internal.util.TimeSpan;

public class AzureFileUploader implements FileUploader {
		
	private static final String CONTENT_TYPE_BINARY = "Binary";
	
	private String account;
	private String key;
	private String container;
	private String hostname;
	
	public AzureFileUploader(String hostname, String account, String key, String container) {
		this.container = container;
		this.hostname = hostname;
		this.account = account;
		this.key = key;
	}
	
	protected synchronized BlobStorageClient getClient() {
		BlobStorageClient client = BlobStorageClient.create(URI.create(hostname), false, account, key);
		client.setRetryPolicy(RetryPolicies.retryN(3, TimeSpan.fromSeconds(5)));
		return client;
	}

	@Override
	public void upload(FileUpload upload) throws Exception {
		
		IBlobProperties properties = new BlobProperties(upload.getFilename());
		properties.setContentType(CONTENT_TYPE_BINARY);
		
		BlobMemoryStream blobStream = new BlobMemoryStream(upload.getFileData());
		IBlobContents blobContents = new BlobContents(blobStream);
		
		IBlobContainer blobContainer = getClient().getBlobContainer(container);
		
		if(blobContainer == null) {
			throw new ConnectException(String.format("Failed to obtain container named %s", container));
		}
		
		if(blobContainer.createBlockBlob(properties, blobContents) == null) {
			throw new Exception("Failed to create blob");
		}
	}
}
