package org.atlasapi.feeds.upload;


public interface FileUploader {

    FileUploadResult upload(FileUpload upload) throws Exception;

}
