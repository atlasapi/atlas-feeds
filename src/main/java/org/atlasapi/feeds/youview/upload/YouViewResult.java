package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;


public class YouViewResult {

    private final String result;
    private final boolean isSuccess;
    
    public static YouViewResult success(String result) {
        return new YouViewResult(result, true);
    }
    
    public static YouViewResult failure(String result) {
        return new YouViewResult(result, false);
    }
    
    private YouViewResult(String result, boolean isSuccess) {
        this.result = checkNotNull(result);
        this.isSuccess = isSuccess;
    }
    
    public String result() {
        return result;
    }
    
    public boolean isSuccess() {
        return isSuccess;
    }
}
