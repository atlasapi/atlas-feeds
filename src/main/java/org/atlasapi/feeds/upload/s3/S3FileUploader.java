package org.atlasapi.feeds.upload.s3;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.FileUploaderResult;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metabroadcast.common.security.UsernameAndPassword;

public class S3FileUploader implements FileUploader {

    private final Logger log = LoggerFactory.getLogger(S3FileUploader.class);
    
    private final AWSCredentials creds;
    private final String bucket;
    private final String folder;

    public S3FileUploader(UsernameAndPassword credentials, String bucket, String folder) {
        this.creds = new AWSCredentials(credentials.username(), credentials.password());
        this.bucket = bucket;
        this.folder = folder;
    }
    
    @Override
    public FileUploaderResult upload(FileUpload upload) throws Exception {
        try {
            S3Service s3Service = new RestS3Service(creds);
            final S3Object object = new S3Object(String.format("%s/%s", folder, upload.getFilename()),upload.getFileData());
            object.setContentType(upload.getContentType().toString());
            s3Service.putObject(s3Service.getBucket(bucket), object);
            return FileUploaderResult.success();
        } catch (S3ServiceException e) {
            log.error(String.format("Exception uploading to s3 bucket: %s, folder: %s", bucket, folder), e);
            return FileUploaderResult.failure().withMessage("" + e);
        }
    }

}
