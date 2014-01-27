package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;

import com.google.common.base.Objects;


public class RadioPlayerHttpsRemoteResult {

    private final FileUploadResultType resultType;
    private final String message;
    
    public RadioPlayerHttpsRemoteResult(FileUploadResultType resultType, String message) {
        this.resultType = checkNotNull(resultType);
        this.message = checkNotNull(message);
    }
    
    public FileUploadResultType getResultType() {
        return resultType;
    }
    
    public String getMessage() {
        return message;
    }
    
    public int hashCode() {
        return Objects.hashCode(resultType, message);
    }
    
    public String toString() {
        return Objects.toStringHelper(RadioPlayerHttpsRemoteResult.class)
                .add("result type", resultType)
                .add("message", message)
                .toString();
    }
    
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof RadioPlayerHttpsRemoteResult) {
            RadioPlayerHttpsRemoteResult other = (RadioPlayerHttpsRemoteResult) that;
            return (resultType.equals(other.resultType))
                    && message.equals(other.message);
        }
        
        return false;
    }
}
