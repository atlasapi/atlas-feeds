package org.atlasapi.feeds.radioplayer.upload;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.feeds.upload.RemoteServiceDetails;
import org.atlasapi.feeds.upload.ftp.CommonsFTPFileUploader;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.security.UsernameAndPassword;


public class RadioPlayerFtpUploadServicesSupplier extends RadioPlayerUploadServicesSupplier {

    private static final String UPLOAD_TYPE_FTP = "ftp";
    
    private final Map<String, RemoteServiceDetails> radioPlayerRemoteServiceDetails;

    public RadioPlayerFtpUploadServicesSupplier(String s3ServiceId, String s3Bucket, UsernameAndPassword s3Credentials, 
            AdapterLog log, XMLValidator validator, Map<String,RemoteServiceDetails> radioPlayerRemoteServiceDetails) {
        super(s3ServiceId, s3Bucket, s3Credentials, log, validator);
        this.radioPlayerRemoteServiceDetails = radioPlayerRemoteServiceDetails;
    }
    
    @Override
    public Iterable<FileUploadService> get(DateTime uploadTime, FileType type) {
        
        return Iterables.concat(
                ImmutableList.of(createS3UploadService(UPLOAD_TYPE_FTP, uploadTime, type)),
                Iterables.transform(radioPlayerRemoteServiceDetails.entrySet(), new Function<Entry<String, RemoteServiceDetails>, FileUploadService>() {
                    @Override
                    public FileUploadService apply(Entry<String, RemoteServiceDetails> input) {
                        return createServiceWithLoggingAndValidation(input.getKey(), new CommonsFTPFileUploader(input.getValue()));
                    }
                })
        );
    }
}
