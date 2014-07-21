package org.atlasapi.feeds.radioplayer.upload.queue;

import org.atlasapi.feeds.upload.FileUpload;

public interface FileUploader {
    
    UploadAttempt upload(FileUpload upload) throws FileUploadException;
}
