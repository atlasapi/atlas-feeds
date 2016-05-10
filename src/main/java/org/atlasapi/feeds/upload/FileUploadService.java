package org.atlasapi.feeds.upload;

import java.net.ConnectException;

import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;

import com.metabroadcast.common.time.DateTimeZones;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class FileUploadService {

    private final String serviceIdentifier;
    private final FileUploader uploader;

    public FileUploadService(String serviceIdentifier, FileUploader uploader) {
        this.serviceIdentifier = checkNotNull(serviceIdentifier);
        this.uploader = checkNotNull(uploader);
    }
    
    public FileUploadResult upload(FileUpload upload) {
        try {
            FileUploaderResult result = uploader.upload(upload);
            FileUploadResult fileUploadResult = new FileUploadResult(
                    serviceIdentifier,
                    upload.getFilename(),
                    new DateTime(DateTimeZones.UTC),
                    result.getStatus()
            );

            if (result.getTransactionId().isPresent()) {
                return fileUploadResult.withTransactionId(result.getTransactionId().get());
            }
            if (result.getMessage().isPresent()) {
                return fileUploadResult.withMessage(result.getMessage().get());
            }
            return fileUploadResult;
        } catch (ConnectException e) {
            return failedUploadResult(upload, e).withConnectionSuccess(false);
        } catch (Exception e) {
            return failedUploadResult(upload, e);
        }
    }

    private FileUploadResult failedUploadResult(FileUpload upload, Exception e) {
        return new FileUploadResult(
                serviceIdentifier,
                upload.getFilename(),
                new DateTime(DateTimeZones.UTC),
                FileUploadResultType.FAILURE
        )
                .withCause(e)
                .withMessage(e.getMessage());
    }

    public String serviceIdentifier() {
        return serviceIdentifier;
    }
}
