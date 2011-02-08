package org.atlasapi.feeds.radioplayer.upload;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.google.common.collect.Ordering;
import com.google.inject.internal.Objects;
import com.metabroadcast.common.time.DateTimeZones;

public class FTPUploadResult {

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

    private final String filename;
    private final DateTime dateTime;
    private final FTPUploadResultType success;
    private String message;
    private Exception exception;
    private ExceptionSummary exceptionSummary;
    private Boolean successfulConnection = true;

    public FTPUploadResult(String filename, DateTime dateTime, FTPUploadResultType success) {
        this.filename = filename;
        this.dateTime = dateTime;
        this.success = success;
    }

    public String filename() {
        return filename;
    }

    public FTPUploadResultType type() {
        return success;
    }

    public DateTime uploadTime() {
        return dateTime;
    }

    public String message() {
        return message;
    }

    public ExceptionSummary exceptionSummary() {
        return exceptionSummary;
    }

    public Exception exception() {
        return exception;
    }

    public Boolean successfulConnection() {
        return successfulConnection;
    }

    public static FTPUploadResult successfulUpload(String filename) {
        return new FTPUploadResult(filename, new DateTime(DateTimeZones.UTC), FTPUploadResultType.SUCCESS);
    }

    public static FTPUploadResult failedUpload(String filename) {
        return new FTPUploadResult(filename, new DateTime(DateTimeZones.UTC), FTPUploadResultType.FAILURE);
    }

    public static FTPUploadResult unknownUpload(String filename) {
        return new FTPUploadResult(filename, new DateTime(DateTimeZones.UTC), FTPUploadResultType.UNKNOWN);
    }

    public FTPUploadResult withMessage(String message) {
        this.message = message;
        return this;
    }

    public FTPUploadResult withCause(Exception e) {
        this.exception = e;
        if (e != null) {
            this.exceptionSummary = new ExceptionSummary(e);
        }
        return this;
    }

    public FTPUploadResult withExceptionSummary(ExceptionSummary summary) {
        exceptionSummary = summary;
        return this;
    }

    public FTPUploadResult withConnectionSuccess(Boolean successfulConnection) {
        this.successfulConnection = successfulConnection;
        return this;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof FTPUploadResult) {
            FTPUploadResult other = (FTPUploadResult) that;
            return Objects.equal(dateTime, other.dateTime) && Objects.equal(filename, filename) && Objects.equal(success, other.success);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dateTime, filename, success);
    }

    @Override
    public String toString() {
        return String.format("%s: %s upload of %s", dateTime.toString("dd/MM/yy HH:mm:ss"), success.toNiceString(), filename);
    }

    public static final Predicate<FTPUploadResult> SUCCESSFUL = new Predicate<FTPUploadResult>() {
        @Override
        public boolean apply(FTPUploadResult input) {
            return FTPUploadResultType.SUCCESS.equals(input.type());
        }
    };
}
