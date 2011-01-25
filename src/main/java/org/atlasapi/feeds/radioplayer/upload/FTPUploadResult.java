package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;

public interface FTPUploadResult {

    public enum FTPUploadResultType {
        SUCCESS("Success"),
        FAILURE("Failure"),
        UNKNOWN("Unknown");
        
        private final String niceName;

        FTPUploadResultType(String niceName) {
            this.niceName = niceName;
        }
        
        public String toNiceString() {
            return niceName;
        }
    }
    
    String filename();

    FTPUploadResultType type();
    
    DateTime uploadTime();
    
    String message();
    
    ExceptionSummary exceptionSummary();
    
    Exception exception();
    
}
