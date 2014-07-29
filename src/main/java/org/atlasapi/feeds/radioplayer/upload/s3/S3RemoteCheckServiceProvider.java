package org.atlasapi.feeds.radioplayer.upload.s3;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploaderProvider.createFolderName;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckService;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckServiceProvider;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.jets3t.service.S3Service;
import org.joda.time.DateTime;

import com.google.common.base.Objects;


public class S3RemoteCheckServiceProvider implements RemoteCheckServiceProvider {

    private final S3Service s3Service;
    private final String s3Bucket;

    public S3RemoteCheckServiceProvider(S3Service s3Service, String s3Bucket) {
        this.s3Service = checkNotNull(s3Service);
        this.s3Bucket = checkNotNull(s3Bucket);
    }
    
    @Override
    public RemoteCheckService get(DateTime uploadTime, FileType type) {
        return new S3RemoteCheckService(s3Service, s3Bucket, createFolderName(uploadTime, type));
    }

    @Override
    public UploadService remoteService() {
        return UploadService.S3;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(s3Service, s3Bucket);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("s3Service", s3Service)
                .add("s3Bucket", s3Bucket)
                .toString();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof S3RemoteCheckServiceProvider) {
            S3RemoteCheckServiceProvider other = (S3RemoteCheckServiceProvider) that;
            return s3Service.equals(other.s3Service)
                    && s3Bucket.equals(other.s3Bucket);
        }
        
        return false;
    }
}
