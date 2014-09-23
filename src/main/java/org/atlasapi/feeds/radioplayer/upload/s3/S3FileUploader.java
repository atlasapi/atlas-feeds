package org.atlasapi.feeds.radioplayer.upload.s3;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.upload.queue.FileUploadException;
import org.atlasapi.feeds.radioplayer.upload.queue.FileUploader;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.upload.FileUpload;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.time.Clock;


public class S3FileUploader implements FileUploader {
    
    public static final String FILENAME_KEY = "filename";
    public static final String HASHCODE_KEY = "hashcode";
    public static final String ERROR_KEY = "error";

    private final Logger log = LoggerFactory.getLogger(S3FileUploader.class);
    
    private final String bucket;
    private final String folder;
    private final Clock clock;

    private final S3Service s3Service;

    public S3FileUploader(S3Service s3Service, String bucket, String folder, Clock clock) {
        this.s3Service = checkNotNull(s3Service);
        this.clock = checkNotNull(clock);
        this.bucket = checkNotNull(bucket);
        this.folder = checkNotNull(folder);
    }
    
    @Override
    public UploadAttempt upload(FileUpload upload) throws FileUploadException {
        try {
            final S3Object object = new S3Object(createObjectName(folder, upload.getFilename()), upload.getFileData());
            object.setContentType(upload.getContentType().toString());
            s3Service.putObject(s3Service.getBucket(bucket), object);
            // TODO can i get any more useful info out of this?
            return UploadAttempt.successfulUpload(clock.now(), ImmutableMap.of(
                    HASHCODE_KEY, object.getMd5HashAsHex(),
                    FILENAME_KEY, upload.getFilename()
            ));
        } catch (S3ServiceException e) {
            log.error(String.format("Exception uploading to s3 bucket: %s, folder: %s", bucket, folder), e);
            return UploadAttempt.failedUpload(clock.now(), ImmutableMap.of(ERROR_KEY, String.valueOf(e)));
        } catch (Exception e) {
            throw new FileUploadException(String.format("Exception creating S3 object %s", upload.toString()), e);
        }
    }

    public static String createObjectName(String s3Folder, String fileName) {
        return String.format("%s/%s", s3Folder, fileName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(s3Service, bucket, folder);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("s3Service", s3Service)
                .add("bucket", bucket)
                .add("folder", folder)
                .toString();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof S3FileUploader) {
            S3FileUploader other = (S3FileUploader) that;
            return s3Service.equals(other.s3Service)
                    && bucket.equals(other.bucket)
                    && folder.equals(other.folder);
        }
        
        return false;
    }
}
