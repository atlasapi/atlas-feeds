package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.persistence.logging.AdapterLog;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.security.UsernameAndPassword;


public class RadioPlayerHttpsUploadServicesSupplier extends RadioPlayerUploadServicesSupplier {
    
    private static final String UPLOAD_TYPE_HTTPS = "https";
    
    private final String httpsServiceId;
    private final SimpleHttpClient httpClient;
    private final String httpsBaseUrl;

    public RadioPlayerHttpsUploadServicesSupplier(boolean uploadToS3Only, String s3ServiceId, String s3Bucket, UsernameAndPassword s3Credentials, AdapterLog log, 
            XMLValidator validator, String httpsServiceId, SimpleHttpClient httpClient, String httpsBaseUrl) {
        super(uploadToS3Only, s3ServiceId, s3Bucket, s3Credentials, log, validator);
        this.httpsServiceId = httpsServiceId;
        this.httpClient = httpClient;
        this.httpsBaseUrl = httpsBaseUrl;
    }
    
    @Override
    Iterable<? extends FileUploadService> createUploadServices() {
        return ImmutableList.of(createServiceWithLoggingAndValidation(
                httpsServiceId, 
                new RadioPlayerHttpsFileUploader(httpClient, httpsBaseUrl)
        ));
    }

    @Override
    String getUploadType() {
        return UPLOAD_TYPE_HTTPS;
    }
}
