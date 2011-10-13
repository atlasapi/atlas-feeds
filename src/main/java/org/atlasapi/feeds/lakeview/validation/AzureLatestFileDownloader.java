package org.atlasapi.feeds.lakeview.validation;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import org.joda.time.DateTime;
import org.soyatec.windowsazure.blob.BlobStorageClient;
import org.soyatec.windowsazure.blob.IBlobContainer;
import org.soyatec.windowsazure.blob.IBlobProperties;
import org.soyatec.windowsazure.blob.internal.RetryPolicies;
import org.soyatec.windowsazure.blob.io.BlobMemoryStream;
import org.soyatec.windowsazure.error.StorageException;
import org.soyatec.windowsazure.error.StorageServerException;
import org.soyatec.windowsazure.internal.util.TimeSpan;

public class AzureLatestFileDownloader {

	private String account;
	private String key;
	private String container;
	private String hostname;

	public AzureLatestFileDownloader(String hostname, String account,
			String key, String container) {
		this.container = container;
		this.hostname = hostname;
		this.account = account;
		this.key = key;
	}

	public AzureFileAndMetadata getLatestFile() {
		IBlobProperties properties = getLastModifiedBlobProperties();
		BlobMemoryStream stream = new BlobMemoryStream();

		getClient().getBlobContainer(container)
				.getBlobReference(properties.getName()).getContents(stream);
		try {
			return new AzureFileAndMetadata(stream.getBytes(), new DateTime(
					properties.getLastModifiedTime()));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected BlobStorageClient getClient() {
		BlobStorageClient client = BlobStorageClient.create(
				URI.create(hostname), false, account, key);
		client.setRetryPolicy(RetryPolicies.retryN(3, TimeSpan.fromSeconds(5)));
		return client;
	}

	private IBlobProperties getLastModifiedBlobProperties() {
		try {
			IBlobContainer blobContainer = getClient().getBlobContainer(
					container);

			Iterator<IBlobProperties> blobProperties = blobContainer
					.listBlobs();
			IBlobProperties latestBlob = null;
			while (blobProperties.hasNext()) {
				IBlobProperties properties = (IBlobProperties) blobProperties
						.next();
				if (latestBlob == null
						|| latestBlob.getLastModifiedTime().before(
								properties.getLastModifiedTime())) {
					latestBlob = properties;
				}
			}
			return latestBlob;
		} catch (StorageServerException e) {
			//
		} catch (StorageException e) {
			//
		}
		return null;
	}
}
