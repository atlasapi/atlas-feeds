package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.metabroadcast.common.time.DateTimeZones;

public class DefaultRadioPlayerUploadResult implements RadioPlayerUploadResult {

    protected boolean success;
    protected String filename;
    protected final DateTime time;
    protected String message;
    protected ExceptionSummary thrown;

    public static DefaultRadioPlayerUploadResult successfulUpload(String filename) {
        return successfulUpload(filename, new DateTime(DateTimeZones.UTC));
    }
    
    public static DefaultRadioPlayerUploadResult successfulUpload(String filename, DateTime time) {
        return new DefaultRadioPlayerUploadResult(filename, time, true);
    }
    
    public static DefaultRadioPlayerUploadResult failedUpload(String filename) {
        return failedUpload(filename, new DateTime(DateTimeZones.UTC));
    }
    
    public static DefaultRadioPlayerUploadResult failedUpload(String filename, DateTime time) {
        return new DefaultRadioPlayerUploadResult(filename, time, false);
    }
    
    protected DefaultRadioPlayerUploadResult(String filename, DateTime time) {
        this.filename = filename;
        this.time = time;
    }
    
    private DefaultRadioPlayerUploadResult(String filename, DateTime time, boolean success) {
        this.filename = filename;
        this.time = time;
        this.success = success;
    }
    
    @Override
    public String filename() {
        return filename;
    }
    
    @Override
    public Boolean wasSuccessful() {
        return success;
    }
    
    @Override
    public DateTime uploadTime() {
        return time;
    }

    @Override
    public String message() {
        return message;
    }

    public DefaultRadioPlayerUploadResult withMessage(String message) {
        this.message = message;
        return this;
    }
    
    @Override
    public ExceptionSummary exception() {
        return thrown;
    }

    public DefaultRadioPlayerUploadResult withCause(Exception thrown) {
        this.thrown = new ExceptionSummary(thrown);
        return this;
    }
    
    public DefaultRadioPlayerUploadResult withCause(ExceptionSummary thrown) {
        this.thrown = thrown;
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s - %s upload of %s", time.toString("dd/MM/yy HH:mm:ss"), wasSuccessful() ? "successful" : "failed", filename);
    }
    
    @Override
    public boolean equals(Object that) {
        if(this == that) {
            return true;
        }
        if(that instanceof DefaultRadioPlayerUploadResult) {
            DefaultRadioPlayerUploadResult other = (DefaultRadioPlayerUploadResult) that;
            return Objects.equal(time, other.time) && Objects.equal(filename, other.filename);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(time, filename);
    }
}
