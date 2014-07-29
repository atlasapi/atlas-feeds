package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;


public class RemoteCheckResult {

    private final FileUploadResultType result;
    private final String message;
    
    public static RemoteCheckResult success(String message) {
        return new RemoteCheckResult(FileUploadResultType.SUCCESS, message);
    }
    
    public static RemoteCheckResult unknown(String message) {
        return new RemoteCheckResult(FileUploadResultType.UNKNOWN, message);
    }
    
    public static RemoteCheckResult failure(String message) {
        return new RemoteCheckResult(FileUploadResultType.FAILURE, message);
    }
    
    private RemoteCheckResult(FileUploadResultType result, String message) {
        this.result = checkNotNull(result);
        this.message = checkNotNull(message);
    }
    
    public FileUploadResultType result() {
        return result;
    }
    
    public String message() {
        return message;
    }
}
