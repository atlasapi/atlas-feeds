package org.atlasapi.feeds.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;

import com.google.common.base.Objects;
import com.google.common.base.Optional;


public class FileUploaderResult {

    private final FileUploadResultType status;
    private Optional<String> transactionId = Optional.absent();
    private Optional<String> message = Optional.absent();

    public static FileUploaderResult success() {
        return new FileUploaderResult(FileUploadResultType.SUCCESS);
    }
    
    public static FileUploaderResult unknown() {
        return new FileUploaderResult(FileUploadResultType.UNKNOWN);
    }
    
    public static FileUploaderResult failure() {
        return new FileUploaderResult(FileUploadResultType.FAILURE);
    }
    
    public FileUploaderResult(FileUploadResultType status) {
        this.status = checkNotNull(status);
    }
    
    public FileUploaderResult withTransactionId(String transactionId) {
        this.transactionId = Optional.fromNullable(transactionId);
        return this;
    }
    
    public FileUploaderResult withMessage(String message) {
        this.message = Optional.fromNullable(message);
        return this;
    }
    
    public FileUploadResultType getStatus() {
        return status;
    }
    
    public Optional<String> getTransactionId() {
        return transactionId;
    }
    
    public Optional<String> getMessage() {
        return message;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(status, transactionId, message);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(FileUploaderResult.class)
                .add("type", status)
                .add("transactionId", transactionId)
                .add("message", message)
                .toString();
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof FileUploaderResult) {
            FileUploaderResult other = (FileUploaderResult) that;
            return status.equals(other.status)
                    && transactionId.equals(other.transactionId)
                    && message.equals(other.message);
        }
        
        return false;
    }
}
