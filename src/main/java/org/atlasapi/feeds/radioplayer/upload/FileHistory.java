package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.collect.MoreSets;


public class FileHistory {
    
    private final RadioPlayerFile file;
    private ImmutableSet<UploadAttempt> uploadAttempts;
    
    private boolean isQueuedForUpload = false;
    private boolean isQueuedForRemoteCheck = false;
    
    public FileHistory(RadioPlayerFile file) {
        this(file, ImmutableSet.<UploadAttempt>of(), false, false);
    }
    public FileHistory(RadioPlayerFile file, Iterable<UploadAttempt> uploadAttempts) {
        this(file, uploadAttempts, false, false);
    }
    
    public FileHistory(RadioPlayerFile file, Iterable<UploadAttempt> uploadAttempts, boolean isQueuedForUpload, boolean isQueuedForRemoteCheck) {
        this.file = checkNotNull(file);
        this.uploadAttempts = ImmutableSet.copyOf(uploadAttempts);
        this.isQueuedForUpload = isQueuedForUpload;
        this.isQueuedForRemoteCheck = isQueuedForRemoteCheck;
    }
    
    public RadioPlayerFile file() {
        return file;
    }
    
    public Set<UploadAttempt> uploadAttempts() {
        return uploadAttempts;
    }
    
    public void addUploadAttempt(UploadAttempt attempt) {
        this.uploadAttempts = MoreSets.add(uploadAttempts, attempt);
    }
    
    public Optional<UploadAttempt> getAttempt(final Long attemptId) {
        return Optional.fromNullable(Iterables.getOnlyElement(
                Iterables.filter(
                        uploadAttempts, 
                        new Predicate<UploadAttempt>() {
                            @Override
                            public boolean apply(UploadAttempt input) {
                                return attemptId.equals(input.id());
                            }
                        }
                ), 
                null
        ));
    }
    
    public UploadAttempt getLatestUpload() {
        return Ordering.<UploadAttempt>natural().max(uploadAttempts);
    }
    
    public boolean isEnqueuedForUpload() {
        return isQueuedForUpload;
    }
    
    public void setEnqueuedForUpload(boolean enqueued) {
        this.isQueuedForUpload = enqueued;
    }

    public boolean isEnqueuedForRemoteCheck() {
        return isQueuedForRemoteCheck;
    }
    
    public void setEnqueuedForRemoteCheck(boolean enqueued) {
        this.isQueuedForRemoteCheck = enqueued;
    }

    /**
     * Copies a FileHistory object, setting the uploadAttempts to 
     * the set of UploadAttempts passed in
     * @param file - the record to copy
     * @param uploadAttempts - the set of upload attempts to 
     * set on the copied object
     * @return the copied object
     */
    public static FileHistory copyWithAttempts(FileHistory file,
            Iterable<UploadAttempt> uploadAttempts) {
        FileHistory newFile = new FileHistory(file.file, uploadAttempts);
        
        newFile.setEnqueuedForUpload(file.isEnqueuedForUpload());
        newFile.setEnqueuedForRemoteCheck(file.isEnqueuedForRemoteCheck());
        
        return newFile;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("file", file)
                .add("uploadAttempts", uploadAttempts)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(file);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof FileHistory) {
            FileHistory other = (FileHistory) that;
            return file.equals(other.file);
        }
        
        return false;
    }

    public void replaceAttempt(UploadAttempt updatedAttempt) {
        if (updatedAttempt.id() == null) {
            return;
        }
        addUploadAttempt(updatedAttempt);
    }
}
