package org.atlasapi.feeds.upload;



public interface FileUploader {

    /**
     * 
     * @param upload the file to upload
     * @return result wrapping the results of the upload
     * @throws Exception
     */
    FileUploaderResult upload(FileUpload upload) throws Exception;

}
