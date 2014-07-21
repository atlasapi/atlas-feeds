package org.atlasapi.feeds.radioplayer.upload.s3;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.queue.FileUploader;
import org.atlasapi.feeds.radioplayer.upload.queue.FileUploaderProvider;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.jets3t.service.S3Service;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.metabroadcast.common.time.Clock;


public class S3FileUploaderProvider implements FileUploaderProvider {

    private static final Joiner JOIN_ON_FORWARD_SLASH = Joiner.on('/').skipNulls();
    private static final DateTimeFormatter DATE_FORMATTER = ISODateTimeFormat.date();
    private static final DateTimeFormatter TIME_FORMATTER = ISODateTimeFormat.hourMinute();
    
    private final S3Service s3Service;
    private final String s3Bucket;
    private final Clock clock;
    
    public S3FileUploaderProvider(S3Service s3Service, String s3Bucket, Clock clock) {
        this.s3Service = checkNotNull(s3Service);
        this.s3Bucket = checkNotNull(s3Bucket);
        this.clock = checkNotNull(clock);
    }
    
    @Override
    public FileUploader get(DateTime uploadTime, FileType type) {
        return new S3FileUploader(s3Service, s3Bucket, createFolderName(uploadTime, type), clock);
    }
    
    /**
     * <p>Creates an S3 folder name according to a certain pattern:
     * [uploaderType]/[fileType]/[Date]/[Time]</p>
     * <p>Date is formatted as yyyy-MM-dd, and Time as HH:mm:ssZZ</p>
     * @param uploaderType
     * @param uploadTime
     * @param fileType
     * @return
     */
    public static String createFolderName(DateTime uploadTime, FileType fileType) {
        return JOIN_ON_FORWARD_SLASH.join(fileType, DATE_FORMATTER.print(uploadTime), TIME_FORMATTER.print(uploadTime));
    }

    @Override
    public UploadService serviceKey() {
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
        if (that instanceof S3FileUploaderProvider) {
            S3FileUploaderProvider other = (S3FileUploaderProvider) that;
            return s3Service.equals(other.s3Service)
                    && s3Bucket.equals(other.s3Bucket);
        }
        
        return false;
    }
}
