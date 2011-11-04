package org.atlasapi.feeds.upload.persistence;

import org.atlasapi.feeds.upload.FileUploadResult;

public interface FileUploadResultStore {

    void store(String service, String identifier, FileUploadResult result);
    
    Iterable<FileUploadResult> result(String service, String identifierPrefix);
    
    Iterable<FileUploadResult> results(String service);
    
}
