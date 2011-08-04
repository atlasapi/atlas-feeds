package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.failedUpload;

import java.io.ByteArrayInputStream;

import org.atlasapi.feeds.xml.XMLValidator;

public class ValidatingFTPFileUploader implements FTPFileUploader {

    private final XMLValidator validator;
    private final FTPFileUploader delegate;

    public ValidatingFTPFileUploader(XMLValidator validator, FTPFileUploader delegate) {
        this.validator = validator;
        this.delegate = delegate;
    }

    @Override
    public FTPUploadResult upload(FTPUpload upload) throws Exception {
        try {
            if (validator != null) {
                validator.validate(new ByteArrayInputStream(upload.getFileData()));
            }
        } catch (Exception e) {
            return failedUpload(upload.getFilename()).withMessage("Failed to validate file").withCause(e).withConnectionSuccess(null);
        }
        return delegate.upload(upload);
    }

}
