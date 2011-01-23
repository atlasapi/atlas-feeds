package org.atlasapi.feeds.radioplayer.upload;

import java.io.ByteArrayInputStream;

public class ValidatingFTPFileUpload implements FTPUpload {

    private final RadioPlayerXMLValidator validator;
    private final FTPUpload delegate;
    private final byte[] fileData;
    private final String filename;

    public ValidatingFTPFileUpload(RadioPlayerXMLValidator validator, String filename, byte[] fileData, FTPUpload delegate) {
        this.validator = validator;
        this.filename = filename;
        this.fileData = fileData;
        this.delegate = delegate;
        
    }

    @Override
    public FTPUploadResult call() throws Exception {
        try{
            validator.validate(new ByteArrayInputStream(fileData));
        } catch (Exception e) {
            return DefaultFTPUploadResult.failedUpload(filename).withMessage("Failed to validate file").withCause(e);
        }
        return delegate.call();
    }

}
