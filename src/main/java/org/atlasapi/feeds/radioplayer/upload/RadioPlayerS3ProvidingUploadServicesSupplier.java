package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.LoggingFileUploader;
import org.atlasapi.feeds.upload.ValidatingFileUploader;
import org.atlasapi.feeds.upload.s3.S3FileUploader;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.metabroadcast.common.security.UsernameAndPassword;

/**
 * Supplies an Iterable<FileUploadService> upon a call to get.
 * This allows the supplied FileUploadServices to be parameterized, in this case with
 * the time of upload and the {@link FileType}.
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public abstract class RadioPlayerS3ProvidingUploadServicesSupplier implements RadioPlayerUploadServicesSupplier {

    private static final Joiner JOIN_ON_FORWARD_SLASH = Joiner.on('/').skipNulls();
    private static final DateTimeFormatter DATE_FORMATTER = ISODateTimeFormat.date();
    private static final DateTimeFormatter TIME_FORMATTER = ISODateTimeFormat.hourMinute();
    
    private final boolean uploadToS3;
    private final boolean uploadToRemote;
    private final String s3ServiceId;
    private final String s3Bucket;
    private final UsernameAndPassword s3Credentials;
    private final AdapterLog log;
    private final XMLValidator validator;

    public RadioPlayerS3ProvidingUploadServicesSupplier(boolean uploadToS3, boolean uploadToRemote, String s3ServiceId, String s3Bucket, UsernameAndPassword s3Credentials, AdapterLog log, XMLValidator validator) {
        this.uploadToS3 = uploadToS3;
        this.uploadToRemote = uploadToRemote;
        this.s3ServiceId = s3ServiceId;
        this.s3Bucket = s3Bucket;
        this.s3Credentials = s3Credentials;
        this.log = log;
        this.validator = validator;
    }

    /**
     * Takes a FileUploader and wraps it with a ValidatingFileUploader and a LoggingFileUploader
     * and returns the resulting FileUploader
     * @param uploader the FileUploader to wrap
     * @return
     */
    protected final FileUploadService createServiceWithLoggingAndValidation(String serviceId, FileUploader uploader) {
        return new FileUploadService(serviceId, new LoggingFileUploader(log, new ValidatingFileUploader(validator, uploader)));
    }

    /**
     * helper method for the main get method, that creates an S3 upload service, parameterised with the type of upload (FTP or HTTPS),
     * the FileType (PI or OD), and the time of upload (usually the time at which a scheduled task is run).
     * @param uploaderType - type of the uploader service. Forms part of the S3 folder name.
     * @param uploadTime - time of upload. Forms part of the S3 folder name
     * @param type - type of the file to be uploaded. Forms part of the S3 folder name.
     * @return
     */
    protected final FileUploadService createS3UploadService(String uploaderType, DateTime uploadTime, FileType type) {
        return new FileUploadService(s3ServiceId, new S3FileUploader(s3Credentials, s3Bucket, createFolderName(uploaderType, uploadTime, type)));
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
    private String createFolderName(String uploaderType, DateTime uploadTime, FileType fileType) {
        return JOIN_ON_FORWARD_SLASH.join(uploaderType, fileType, DATE_FORMATTER.print(uploadTime), TIME_FORMATTER.print(uploadTime));
    }
    

    
    /* (non-Javadoc)
     * @see org.atlasapi.feeds.radioplayer.upload.UploadServicesSupplier#get(org.joda.time.DateTime, org.atlasapi.feeds.radioplayer.upload.FileType)
     */
    @Override
    public final Iterable<FileUploadService> get(DateTime uploadTime, FileType type) {
        Builder<FileUploadService> services = ImmutableSet.<FileUploadService>builder();
        if (uploadToS3) {
            services.add(createS3UploadService(getUploadType(), uploadTime, type));
        } 
        if (uploadToRemote) {
            services.addAll(createUploadServices());
        }
        return services.build();
    }

    abstract Iterable<? extends FileUploadService> createUploadServices();
    
    abstract String getUploadType();
}
