package org.atlasapi.feeds.upload.azure;

import java.net.URI;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.soyatec.windowsazure.blob.BlobStorageClient;
import org.soyatec.windowsazure.blob.IBlobContainer;
import org.soyatec.windowsazure.blob.IBlobContents;
import org.soyatec.windowsazure.blob.IBlobProperties;
import org.soyatec.windowsazure.blob.internal.BlobContents;
import org.soyatec.windowsazure.blob.internal.BlobProperties;
import org.soyatec.windowsazure.blob.internal.RetryPolicies;
import org.soyatec.windowsazure.blob.io.BlobMemoryStream;
import org.soyatec.windowsazure.error.StorageException;
import org.soyatec.windowsazure.error.StorageServerException;
import org.soyatec.windowsazure.internal.util.TimeSpan;

import static org.atlasapi.feeds.upload.FileUploadResult.failedUpload;
import static org.atlasapi.feeds.upload.FileUploadResult.successfulUpload;

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
	public FileUploadResult upload(FileUpload upload) {
		
		try {
			IBlobProperties properties = new BlobProperties(upload.getFilename());
			properties.setContentType(CONTENT_TYPE_BINARY);
			
			BlobMemoryStream blobStream = new BlobMemoryStream(upload.getFileData());
			IBlobContents blobContents = new BlobContents(blobStream);
			
			IBlobContainer blobContainer = getClient().getBlobContainer(container);
			
			if(blobContainer == null) {
				return failedUpload(upload.getFilename()).withMessage("Could not get container: " + container);
			}
			
			if(blobContainer.createBlockBlob(properties, blobContents) == null) {
				return failedUpload(upload.getFilename()).withMessage("Could not create block blob").withConnectionSuccess(true);
			}
			return successfulUpload(upload.getFilename()).withMessage("File uploaded successfully");
		}
		catch(StorageServerException e) {
			return failedUpload(upload.getFilename()).withCause(e);
		} 
		catch (StorageException e) {
			return failedUpload(upload.getFilename()).withCause(e);
		}
	}
	


}
