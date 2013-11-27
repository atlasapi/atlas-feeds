package org.atlasapi.feeds.upload;

import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;

public class ResultStoringFileUploader implements FileUploader {

	private FileUploadResultStore store;
	private FileUploader delegate;
	private String identifier;
	private String remoteId;
	
	public static ResultStoringFileUploader resultStoringFileUploader(FileUploadResultStore store, String identifier, String remoteId, FileUploader delegate) {
		return new ResultStoringFileUploader(store, identifier, remoteId, delegate);
	}
	
	public ResultStoringFileUploader(FileUploadResultStore store, String identifier, String remoteId, FileUploader delegate) {
		this.store = store;
		this.delegate = delegate;
		this.identifier = identifier;
		this.remoteId = remoteId;
	}
	
	@Override
	public FileUploaderResult upload(FileUpload upload) throws Exception {
		try {
			FileUploaderResult result = delegate.upload(upload);
			store.store(identifier, FileUploadResult.successfulUpload(remoteId, upload.getFilename()));
			return result;
		}
		catch (Exception e) {
			store.store(identifier, FileUploadResult.failedUpload(remoteId, upload.getFilename()));
			throw e;
		}
	}

}
