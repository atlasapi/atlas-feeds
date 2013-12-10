package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerFilenameMatcher;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.FileUploaderResult;
import org.mortbay.log.Log;

import com.metabroadcast.common.http.BytesPayload;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;


public class RadioPlayerHttpsFileUploader implements FileUploader {
    
    // If RadioPlayer want the file to be retried, they send a 503, with a header of 'Retry-After' and a 
    // retry time in seconds.
    private static final int RETRY_AFTER = 503;
    private static final int ACCEPTED = 202;
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String LOCATION_HEADER = "Location";
    private static final int RETRY_LOG_INTERVAL = 5;
    private final SimpleHttpClient httpClient;
    private final String baseUrl;

    public RadioPlayerHttpsFileUploader(SimpleHttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public FileUploaderResult upload(FileUpload upload) throws Exception {
        
        HttpResponse response = postFileData(upload);
        int retries = 0;
        while (response.statusCode() == RETRY_AFTER) {
            String retryAfterHeader = response.header(RETRY_AFTER_HEADER);
            if (retryAfterHeader == null) {
                break;
            }
            int retry = Integer.parseInt(retryAfterHeader);
            Thread.sleep(retry);
            response = postFileData(upload);
            retries++;
            if (retries % RETRY_LOG_INTERVAL == 0) {
                Log.info(String.format("Retried upload %s times for file %s", retries, upload.getFilename()));
            }
        }
        if (response.statusCode() == ACCEPTED) {
            FileUploaderResult result = new FileUploaderResult(FileUploadResultType.SUCCESS).withTransactionId(response.header(LOCATION_HEADER));
            return (retries > 0) ? result.withMessage(String.format("Uploaded after %d Retries", retries)) : result;
        }
        return FileUploaderResult.failure()
            .withMessage(response.statusCode() + ": " + response.statusLine());
    }

    private HttpResponse postFileData(FileUpload upload) throws HttpException {
        String queryUrl = baseUrl + "/" + getFileType(upload) + "/";
        return httpClient.post(queryUrl, new BytesPayload(upload.getFileData()));
    }

    private String getFileType(FileUpload file) {
        RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(file.getFilename().trim().replace(".xml", ""));
        if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {
            return matcher.type().requireValue().name().toLowerCase();
        }
        throw new RuntimeException("filename " + file + " does not match the standard pattern");
    }

}
