package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.security.UsernameAndPassword;


public class RadioPlayerHttpsUploadServicesSupplier extends RadioPlayerUploadServicesSupplier {
    
    private static final String UPLOAD_TYPE_HTTPS = "https";
    
    private final String httpsServiceId;
    private final SimpleHttpClient httpClient;
    private final String httpsBaseUrl;
    private final boolean createHttpsUploadService;

    public RadioPlayerHttpsUploadServicesSupplier(boolean createHttpsUploadService, String s3ServiceId, String s3Bucket, UsernameAndPassword s3Credentials, AdapterLog log, 
            XMLValidator validator, String httpsServiceId, SimpleHttpClient httpClient, String httpsBaseUrl) {
        super(s3ServiceId, s3Bucket, s3Credentials, log, validator);
        this.createHttpsUploadService = createHttpsUploadService;
        this.httpsServiceId = httpsServiceId;
        this.httpClient = httpClient;
        this.httpsBaseUrl = httpsBaseUrl;
    }
    
    @Override
    public Iterable<FileUploadService> get(DateTime uploadTime, FileType type) {
        Builder<FileUploadService> uploadServices = ImmutableSet.<FileUploadService>builder();
        if (createHttpsUploadService) {
            uploadServices.add(createServiceWithLoggingAndValidation(httpsServiceId, new RadioPlayerHttpsFileUploader(httpClient, httpsBaseUrl)));
        }
        uploadServices.add(createS3UploadService(UPLOAD_TYPE_HTTPS, uploadTime, type));
        return uploadServices.build();
    }
}
