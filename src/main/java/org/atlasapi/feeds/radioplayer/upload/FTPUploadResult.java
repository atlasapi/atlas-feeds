package org.atlasapi.feeds.radioplayer.upload;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;

import com.google.common.collect.Ordering;

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
        
        public static final List<FTPUploadResultType> RESULT_TYPES = Arrays.asList(values());
    }
    
    public static final Ordering<FTPUploadResult> DATE_ORDERING = Ordering.from(new Comparator<FTPUploadResult>() {
        @Override
        public int compare(FTPUploadResult r1, FTPUploadResult r2) {
            return r1.uploadTime().compareTo(r2.uploadTime());
        }
    });
    
    public static final Ordering<FTPUploadResult> TYPE_ORDERING = Ordering.from(new Comparator<FTPUploadResult>() {
        @Override
        public int compare(FTPUploadResult r1, FTPUploadResult r2) {
            return r1.type().compareTo(r2.type());
        }
    });

    
    String filename();

    FTPUploadResultType type();
    
    DateTime uploadTime();
    
    String message();
    
    ExceptionSummary exceptionSummary();
    
    Exception exception();
    
}
