package org.atlasapi.feeds.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;

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
    public void upload(FileUpload upload) throws Exception {
        validator.validate(new ByteArrayInputStream(upload.getFileData()));
        delegate.upload(upload);
    }

}
