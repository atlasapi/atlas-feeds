package org.atlasapi.feeds.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.failedUpload;

import java.io.ByteArrayInputStream;

import org.atlasapi.feeds.xml.XMLValidator;

public class ValidatingFileUploader implements FileUploader {

    private final XMLValidator validator;
    private final FileUploader delegate;

    public ValidatingFileUploader(XMLValidator validator, FileUploader delegate) {
        this.validator = validator;
        this.delegate = delegate;
    }

    @Override
    public FileUploadResult upload(FileUpload upload) throws Exception {
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
