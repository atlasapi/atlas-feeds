package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.Nullable;

import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;


public class UploadAttempt implements Comparable<UploadAttempt> {
    
    private Long id;
    private final DateTime uploadTime;
    private final FileUploadResultType uploadResult;
    private final Map<String, String> uploadDetails;
    private final FileUploadResultType remoteCheckResult;
    private final String remoteCheckMessage;
    
    public static UploadAttempt enqueuedAttempt() {
        return new UploadAttempt(null, FileUploadResultType.UNKNOWN, ImmutableMap.<String, String>of(), null, null);
    }
    
    public static UploadAttempt successfulUpload(DateTime uploadTime, Map<String, String> uploadDetails) {
        return new UploadAttempt(uploadTime, FileUploadResultType.SUCCESS, uploadDetails, null, null);
    }
    
    public static UploadAttempt failedUpload(DateTime uploadTime, Map<String, String> uploadDetails) {
        return new UploadAttempt(uploadTime, FileUploadResultType.FAILURE, uploadDetails, null, null);
    }
    
    public static UploadAttempt successfulRemoteCheck(UploadAttempt attempt) {
        return new UploadAttempt(attempt.id(), attempt.uploadTime(), attempt.uploadResult(), attempt.uploadDetails(), 
                FileUploadResultType.SUCCESS, "successfully processed");
    }
    
    public static UploadAttempt failedRemoteCheck(UploadAttempt attempt, String remoteCheckMessage) {
        return new UploadAttempt(attempt.id(), attempt.uploadTime(), attempt.uploadResult(), attempt.uploadDetails(), 
                FileUploadResultType.FAILURE, remoteCheckMessage);
    }
    
    public static UploadAttempt unknownRemoteCheck(UploadAttempt attempt, String remoteCheckMessage) {
        return new UploadAttempt(attempt.id(), attempt.uploadTime(), attempt.uploadResult(), attempt.uploadDetails(), 
                FileUploadResultType.UNKNOWN, remoteCheckMessage);
    }
    
    public UploadAttempt(@Nullable DateTime uploadTime, FileUploadResultType uploadResult,
            Map<String, String> uploadDetails, @Nullable FileUploadResultType remoteCheckResult, 
            @Nullable String remoteCheckMessage) {
        this.id = null;
        this.uploadTime = uploadTime;
        this.uploadResult = checkNotNull(uploadResult);
        this.uploadDetails = ImmutableMap.copyOf(uploadDetails);
        this.remoteCheckResult = remoteCheckResult;
        this.remoteCheckMessage = remoteCheckMessage;
    }
    
    public UploadAttempt(Long id, @Nullable DateTime uploadTime, FileUploadResultType uploadResult,
            Map<String, String> uploadDetails, @Nullable FileUploadResultType remoteCheckResult, 
            @Nullable String remoteCheckMessage) {
        this.id = checkNotNull(id);
        this.uploadTime = uploadTime;
        this.uploadResult = checkNotNull(uploadResult);
        this.uploadDetails = ImmutableMap.copyOf(uploadDetails);
        this.remoteCheckResult = remoteCheckResult;
        this.remoteCheckMessage = remoteCheckMessage;
    }
    
    public Long id() {
        return id;
    }
    
    public DateTime uploadTime() {
        return uploadTime;
    }
    
    public FileUploadResultType uploadResult() {
        return uploadResult;
    }
    
    public Map<String, String> uploadDetails() {
        return uploadDetails;
    }
    
    public FileUploadResultType remoteCheckResult() {
        return remoteCheckResult;
    }
    
    public String remoteCheckMessage() {
        return remoteCheckMessage;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(uploadTime, uploadResult, uploadDetails, remoteCheckResult, remoteCheckMessage);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("id", id)
                .add("uploadTime", uploadTime)
                .add("uploadResult", uploadResult)
                .add("uploadDetails", uploadDetails)
                .add("remoteCheckResult", remoteCheckResult)
                .add("remoteCheckMessage", remoteCheckMessage)
                .toString();
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof UploadAttempt) {
            UploadAttempt other = (UploadAttempt) that;
            
            if (id != null) {
                return id.equals(other.id);
            }
            
            return Objects.equal(uploadTime, other.uploadTime)
                    && Objects.equal(uploadResult, other.uploadResult)
                    && Objects.equal(uploadDetails, other.uploadDetails)
                    && Objects.equal(remoteCheckResult, other.remoteCheckResult)
                    && Objects.equal(remoteCheckMessage, other.remoteCheckMessage);
        }
        
        return false;
    }
    
    public UploadAttempt copyWithId(Long id) {
        return new UploadAttempt(id, uploadTime, uploadResult, uploadDetails, remoteCheckResult, remoteCheckMessage);
    }
    
    public static Predicate<UploadAttempt> matchesId(final Long id) { 
        return new Predicate<UploadAttempt>() {
        @Override
        public boolean apply(UploadAttempt input) {
            return input.equals(id);
        }
    };
    }

    @Override
    public int compareTo(UploadAttempt that) {
        return ComparisonChain.start().compare(this.uploadTime(), that.uploadTime(), Ordering.natural().nullsFirst()).result();
    };
}
