package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;

import nu.xom.ValidityException;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.xml.XMLValidator;

public class ValidatingFileUploader implements FileUploader {

    public static ValidatingFileUploader validatingUploader(XMLValidator validator, FileUploader delegate) {
        return new ValidatingFileUploader(validator, delegate);
    }
    
    private final XMLValidator validator;
    private final FileUploader delegate;

    public ValidatingFileUploader(XMLValidator validator, FileUploader delegate) {
        this.validator = checkNotNull(validator);
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public UploadAttempt upload(FileUpload upload) throws FileUploadException {
        try {
            validator.validate(new ByteArrayInputStream(upload.getFileData()));
            return delegate.upload(upload);
        } catch (ValidityException e) {
            throw new FileUploadException("validation error", e); 
        }
    }

    public FileUploader delegate() {
        return delegate;
    }
}
