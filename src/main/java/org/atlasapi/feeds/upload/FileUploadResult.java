package org.atlasapi.feeds.upload;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.google.common.collect.Ordering;
import com.google.inject.internal.Objects;
import com.metabroadcast.common.time.DateTimeZones;

public class FileUploadResult {

    public enum FileUploadResultType {
        SUCCESS("Success"), 
        FAILURE("Failure"), 
        UNKNOWN("Unknown");

        private final String niceName;

        FileUploadResultType(String niceName) {
            this.niceName = niceName;
        }

        public String toNiceString() {
            return niceName;
        }

        public static final List<FileUploadResultType> RESULT_TYPES = Arrays.asList(values());
    }

    public static final Ordering<FileUploadResult> DATE_ORDERING = Ordering.from(new Comparator<FileUploadResult>() {
        @Override
        public int compare(FileUploadResult r1, FileUploadResult r2) {
            return r1.uploadTime().compareTo(r2.uploadTime());
        }
    });

    public static final Ordering<FileUploadResult> TYPE_ORDERING = Ordering.from(new Comparator<FileUploadResult>() {
        @Override
        public int compare(FileUploadResult r1, FileUploadResult r2) {
            return r1.type().compareTo(r2.type());
        }
    });

    private final String filename;
    private final DateTime dateTime;
    private final FileUploadResultType success;
    private String message;
    private Exception exception;
    private ExceptionSummary exceptionSummary;
    private Boolean successfulConnection = true;

    public FileUploadResult(String filename, DateTime dateTime, FileUploadResultType success) {
        this.filename = filename;
        this.dateTime = dateTime;
        this.success = success;
    }

    public String filename() {
        return filename;
    }

    public FileUploadResultType type() {
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

    public static FileUploadResult successfulUpload(String filename) {
        return new FileUploadResult(filename, new DateTime(DateTimeZones.UTC), FileUploadResultType.SUCCESS);
    }

    public static FileUploadResult failedUpload(String filename) {
        return new FileUploadResult(filename, new DateTime(DateTimeZones.UTC), FileUploadResultType.FAILURE);
    }

    public static FileUploadResult unknownUpload(String filename) {
        return new FileUploadResult(filename, new DateTime(DateTimeZones.UTC), FileUploadResultType.UNKNOWN);
    }

    public FileUploadResult withMessage(String message) {
        this.message = message;
        return this;
    }

    public FileUploadResult withCause(Exception e) {
        this.exception = e;
        if (e != null) {
            this.exceptionSummary = new ExceptionSummary(e);
        }
        return this;
    }

    public FileUploadResult withExceptionSummary(ExceptionSummary summary) {
        exceptionSummary = summary;
        return this;
    }

    public FileUploadResult withConnectionSuccess(Boolean successfulConnection) {
        this.successfulConnection = successfulConnection;
        return this;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof FileUploadResult) {
            FileUploadResult other = (FileUploadResult) that;
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

    public static final Predicate<FileUploadResult> SUCCESSFUL = new Predicate<FileUploadResult>() {
        @Override
        public boolean apply(FileUploadResult input) {
            return FileUploadResultType.SUCCESS.equals(input.type());
        }
    };
}
