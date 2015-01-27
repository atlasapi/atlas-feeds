package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.DateTime;


public class YouViewResult {

    private final String result;
    private final DateTime uploadTime;
    private final Integer responseCode;
    private final boolean isSuccess;
    
    public static YouViewResult success(String result, DateTime uploadTime, Integer responseCode) {
        return new YouViewResult(result, uploadTime, responseCode, true);
    }
    
    public static YouViewResult failure(String result, DateTime uploadTime, Integer responseCode) {
        return new YouViewResult(result, uploadTime, responseCode, false);
    }
    
    private YouViewResult(String result, DateTime uploadTime, Integer responseCode, boolean isSuccess) {
        this.result = checkNotNull(result);
        this.uploadTime = checkNotNull(uploadTime);
        this.responseCode = checkNotNull(responseCode);
        this.isSuccess = isSuccess;
    }
    
    public String result() {
        return result;
    }
    
    public DateTime uploadTime() {
        return uploadTime;
    }
    
    public Integer responseCode() {
        return responseCode;
    }
    
    public boolean isSuccess() {
        return isSuccess;
    }
}
