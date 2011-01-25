package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.google.inject.internal.Objects;
import com.metabroadcast.common.time.DateTimeZones;

public class DefaultFTPUploadResult implements FTPUploadResult {

    private final String filename;
    private final DateTime dateTime;
    private final FTPUploadResultType success;
    private String message;
    private Exception exception;
    private ExceptionSummary exceptionSummary;

    public DefaultFTPUploadResult(String filename, DateTime dateTime, FTPUploadResultType success) {
        this.filename = filename;
        this.dateTime = dateTime;
        this.success = success;
    }

    @Override
    public String filename() {
        return filename;
    }

    @Override
    public FTPUploadResultType type() {
        return success;
    }

    @Override
    public DateTime uploadTime() {
        return dateTime;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public ExceptionSummary exceptionSummary() {
        return exceptionSummary;
    }

    @Override
    public Exception exception() {
        return exception;
    }

    public static DefaultFTPUploadResult successfulUpload(String filename) {
        return new DefaultFTPUploadResult(filename, new DateTime(DateTimeZones.UTC), FTPUploadResultType.SUCCESS);
    }

    public static DefaultFTPUploadResult failedUpload(String filename) {
        return new DefaultFTPUploadResult(filename, new DateTime(DateTimeZones.UTC), FTPUploadResultType.FAILURE);
    }

    public static DefaultFTPUploadResult unknownUpload(String filename) {
        return new DefaultFTPUploadResult(filename, new DateTime(DateTimeZones.UTC), FTPUploadResultType.UNKNOWN);
    }

    public DefaultFTPUploadResult withMessage(String message) {
        this.message = message;
        return this;
    }

    public DefaultFTPUploadResult withCause(Exception e) {
        this.exception = e;
        this.exceptionSummary = new ExceptionSummary(e);
        return this;
    }

    public DefaultFTPUploadResult withExceptionSummary(ExceptionSummary summary) {
        exceptionSummary = summary;
        return this;
    }

    @Override
    public boolean equals(Object that) {
        if(this == that) {
            return true;
        }
        if(that instanceof DefaultFTPUploadResult) {
            DefaultFTPUploadResult other = (DefaultFTPUploadResult) that;
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

    public static final Predicate<FTPUploadResult> IS_SUCCESS = new Predicate<FTPUploadResult>() {
        @Override
        public boolean apply(FTPUploadResult input) {
            return FTPUploadResultType.SUCCESS.equals(input.type());
        }
    };
}
