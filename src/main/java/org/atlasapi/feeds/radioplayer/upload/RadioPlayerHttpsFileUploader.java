package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerFilenameMatcher;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.FileUploaderResult;

import com.metabroadcast.common.http.BytesPayload;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;


public class RadioPlayerHttpsFileUploader implements FileUploader {
    
    private static final int RETRY_AFTER = 503;
    private static final int ACCEPTED = 202;
    private static final String RETRY_AFTER_HEADER = "RetryAfter";
    private static final String LOCATION_HEADER = "Location";
    private final SimpleHttpClient httpClient;
    private final String baseUrl;

    public RadioPlayerHttpsFileUploader(SimpleHttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public FileUploaderResult upload(FileUpload upload) throws Exception {
        String queryUrl = baseUrl + "/" + getFileType(upload) + "/";
        HttpResponse response = httpClient.post(queryUrl, new BytesPayload(upload.getFileData()));
        if (response.statusCode() == ACCEPTED) {
            return new FileUploaderResult(FileUploadResultType.SUCCESS).withTransactionId(response.header(LOCATION_HEADER));
        } else if (response.statusCode() == RETRY_AFTER) {
            return new FileUploaderResult(FileUploadResultType.FAILURE).withMessage(response.header(RETRY_AFTER_HEADER));
        }
        return FileUploaderResult.failure()
            .withMessage(response.statusCode() + ": " + response.statusLine());
    }

    private String getFileType(FileUpload file) {
        RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(file.getFilename().trim().replace(".xml", ""));
        if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {
            return matcher.type().requireValue().name().toLowerCase();
        }
        throw new RuntimeException("filename " + file + " does not match the standard pattern");
    }

}
