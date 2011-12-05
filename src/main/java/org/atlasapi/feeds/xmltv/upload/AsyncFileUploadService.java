package org.atlasapi.feeds.xmltv.upload;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncFileUploadService {

    public ListenableFuture<FileUploadResult> upload(FileUpload upload);
    
    public String serviceName();
    
}
