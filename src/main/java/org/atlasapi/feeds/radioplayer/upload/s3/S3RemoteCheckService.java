package org.atlasapi.feeds.radioplayer.upload.s3;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploader.FILENAME_KEY;
import static org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploader.HASHCODE_KEY;
import static org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploader.createObjectName;

import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckException;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckResult;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckService;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckTask;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;


public class S3RemoteCheckService implements RemoteCheckService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final S3Service s3Service;
    private final String s3Bucket;
    private final String s3Folder;
    
    public S3RemoteCheckService(S3Service s3Service, String s3Bucket, String s3Folder) {
        this.s3Service = checkNotNull(s3Service);
        this.s3Bucket = checkNotNull(s3Bucket);
        this.s3Folder = checkNotNull(s3Folder);
    }
    
    @Override
    public RemoteCheckResult check(RemoteCheckTask task) throws RemoteCheckException {
        try {
            // TODO what does this do when object doesn't exist?
            S3Object object = s3Service.getObject(s3Bucket, createObjectName(s3Folder, task.getParameter(FILENAME_KEY).get()));
            Optional<String> recordedHash = task.getParameter(HASHCODE_KEY);
            if (!recordedHash.isPresent()) {
                return RemoteCheckResult.failure(String.format("No recorded hash, so cannot check against uploaded object"));
            }
            if (object.getMd5HashAsHex().equals(recordedHash.get())) {
                return RemoteCheckResult.success("Object exists in S3 and has correct hash");
            } 
            return RemoteCheckResult.failure(String.format("Object is present, but hash %s does not match recorded hash %s", object.getMd5HashAsHex(), recordedHash.get()));
        } catch (S3ServiceException e) {
            log.error("Error checking S3 for task {}: {}", task, e);
            return RemoteCheckResult.failure(String.format("Error checking S3 for task", e));
        }
    }

    @Override
    public UploadService remoteService() {
        return UploadService.S3;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(s3Service, s3Bucket, s3Folder);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("s3Service", s3Service)
                .add("s3Bucket", s3Bucket)
                .add("s3Folder", s3Folder)
                .toString();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof S3RemoteCheckService) {
            S3RemoteCheckService other = (S3RemoteCheckService) that;
            return s3Service.equals(other.s3Service)
                    && s3Bucket.equals(other.s3Bucket)
                    && s3Folder.equals(other.s3Folder);
        }
        
        return false;
    }
}
