package org.atlasapi.feeds.radioplayer.upload;

import java.io.ByteArrayInputStream;

import org.apache.commons.net.ftp.FTPClient;

public class ValidatingFTPFileUpload implements FTPUpload {

    private final RadioPlayerXMLValidator validator;
    private final FTPUpload delegate;

    public ValidatingFTPFileUpload(RadioPlayerXMLValidator validator, FTPUpload delegate) {
        this.validator = validator;
        this.delegate = delegate;
    }

    @Override
    public FTPUploadResult upload(FTPClient client, String filename, byte[] fileData) {
        try{
            if(validator != null) {
                validator.validate(new ByteArrayInputStream(fileData));
            }
        } catch (Exception e) {
            return DefaultFTPUploadResult.failedUpload(filename).withMessage("Failed to validate file").withCause(e);
        }
        return delegate.upload(client, filename, fileData);
    }

}
