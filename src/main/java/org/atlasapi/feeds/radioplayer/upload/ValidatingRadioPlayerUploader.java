package org.atlasapi.feeds.radioplayer.upload;

import java.io.ByteArrayInputStream;

import nu.xom.ValidityException;

public class ValidatingRadioPlayerUploader implements RadioPlayerUploader {

    private final RadioPlayerUploader delegate;
    private final RadioPlayerXMLValidator validator;

    public ValidatingRadioPlayerUploader(RadioPlayerXMLValidator validator, RadioPlayerUploader delegate) {
        this.validator = validator;
        this.delegate = delegate;
    }
    
    @Override
    public RadioPlayerUploadResult upload(String filename, byte[] fileData) {
        if(validator != null) {
            try {
                validator.validate(new ByteArrayInputStream(fileData));
            } catch(ValidityException ve) {
                return DefaultRadioPlayerUploadResult.failedUpload(filename).withMessage("Invalid file").withCause(ve);
            }
        }
        return delegate.upload(filename, fileData);
    }
   
}
