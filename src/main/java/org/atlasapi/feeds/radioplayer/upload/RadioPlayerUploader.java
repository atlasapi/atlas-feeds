package org.atlasapi.feeds.radioplayer.upload;

public interface RadioPlayerUploader {
    
    RadioPlayerUploadResult upload(String filename, byte[] fileData);
    
}
