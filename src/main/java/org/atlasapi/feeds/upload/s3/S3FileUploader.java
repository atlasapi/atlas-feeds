package org.atlasapi.feeds.upload.s3;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.FileUploaderResult;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

import com.metabroadcast.common.security.UsernameAndPassword;

public class S3FileUploader implements FileUploader {

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
        S3Service s3Service = new RestS3Service(creds);
        final S3Object object = new S3Object(String.format("%s/%s", folder, upload.getFilename()),upload.getFileData());
        object.setContentType(upload.getContentType().toString());
        s3Service.putObject(s3Service.getBucket(bucket), object);
        return FileUploaderResult.success();
    }

}
