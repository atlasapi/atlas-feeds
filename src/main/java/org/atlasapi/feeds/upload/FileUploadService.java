package org.atlasapi.feeds.upload;

import java.net.ConnectException;

import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.DateTime;

import com.metabroadcast.common.time.DateTimeZones;

public class FileUploadService {

    private final String serviceIdentifier;
    private final FileUploader uploader;

    public FileUploadService(String serviceIdentifier, FileUploader uploader) {
        this.serviceIdentifier = serviceIdentifier;
        this.uploader = uploader;
    }
    
    public FileUploadResult upload(FileUpload upload) {
        try {
            uploader.upload(upload);
            return new FileUploadResult(upload.getFilename(), new DateTime(DateTimeZones.UTC), FileUploadResultType.SUCCESS);
        } catch (ConnectException e) {
            return failedUploadResult(upload, e).withConnectionSuccess(false);
        } catch (Exception e) {
            return failedUploadResult(upload, e);
        }
    }

    private FileUploadResult failedUploadResult(FileUpload upload, Exception e) {
        return new FileUploadResult(upload.getFilename(), new DateTime(DateTimeZones.UTC), FileUploadResultType.FAILURE).withCause(e);
    }

    public String serviceIdentifier() {
        return serviceIdentifier;
    }
    
}
