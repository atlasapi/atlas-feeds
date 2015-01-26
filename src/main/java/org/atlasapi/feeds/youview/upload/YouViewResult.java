package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.DateTime;


public class YouViewResult {

    private final String result;
    private final DateTime uploadTime;
    private final boolean isSuccess;
    
    public static YouViewResult success(String result, DateTime uploadTime) {
        return new YouViewResult(result, uploadTime, true);
    }
    
    public static YouViewResult failure(String result, DateTime uploadTime) {
        return new YouViewResult(result, uploadTime, false);
    }
    
    private YouViewResult(String result, DateTime uploadTime, boolean isSuccess) {
        this.result = checkNotNull(result);
        this.uploadTime = checkNotNull(uploadTime);
        this.isSuccess = isSuccess;
    }
    
    public String result() {
        return result;
    }
    
    public DateTime uploadTime() {
        return uploadTime;
    }
    
    public boolean isSuccess() {
        return isSuccess;
    }
}
