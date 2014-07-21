package org.atlasapi.feeds.radioplayer.upload.https;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.RadioPlayerFilenameMatcher;
import org.atlasapi.feeds.radioplayer.upload.queue.FileUploadException;
import org.atlasapi.feeds.radioplayer.upload.queue.FileUploader;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.upload.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.BytesPayload;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.time.Clock;


public class HttpsFileUploader implements FileUploader {

    public static final String STATUS_LINE_KEY = "statusLine";
    public static final String RESPONSE_CODE_KEY = "responseCode";
    public static final String RETRIES_KEY = "retries";
    public static final String TRANSACTION_ID_KEY = "transactionId";
    // If RadioPlayer want the file to be retried, they send a 503, with a header of 'Retry-After' and a 
    // retry time in seconds.
    private static final int RETRY_AFTER = 503;
    private static final int ACCEPTED = 202;
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String LOCATION_HEADER = "Location";
    private static final int RETRY_LOG_INTERVAL = 5;
    private static final int DEFAULT_RETRY_TIME = 30;
    
    private final Logger log = LoggerFactory.getLogger(HttpsFileUploader.class);
    private final SimpleHttpClient httpClient;
    private final String baseUrl;
    private final Clock clock;

    public HttpsFileUploader(SimpleHttpClient httpClient, String baseUrl, Clock clock) {
        this.httpClient = checkNotNull(httpClient);
        this.baseUrl = checkNotNull(baseUrl);
        this.clock = checkNotNull(clock);
    }

    @Override
    public UploadAttempt upload(FileUpload upload) throws FileUploadException {
        
        HttpResponse response;
        try {
            response = postFileData(upload);
            int retries = 0;
            while (response.statusCode() == RETRY_AFTER) {
                String retryAfterHeader = response.header(RETRY_AFTER_HEADER);
                int retry;
                if (retryAfterHeader != null) {
                    retry = Integer.parseInt(retryAfterHeader);
                } else {
                    retry = DEFAULT_RETRY_TIME;
                }
                Thread.sleep(retry * 1000);
                response = postFileData(upload);
                retries++;
                if (retries % RETRY_LOG_INTERVAL == 0) {
                    log.info(String.format("Retried upload %s times for file %s", retries, upload.getFilename()));
                }
            }
            if (response.statusCode() == ACCEPTED) {
                return UploadAttempt.successfulUpload(clock.now(), ImmutableMap.of(TRANSACTION_ID_KEY, response.header(LOCATION_HEADER), RETRIES_KEY, String.valueOf(retries)));
            }
            if (response.statusCode() == RETRY_AFTER) {
                return UploadAttempt.failedUpload(clock.now(), ImmutableMap.of(RESPONSE_CODE_KEY, String.valueOf(response.statusCode()), RETRIES_KEY, String.valueOf(retries)));
            }
            return UploadAttempt.failedUpload(clock.now(), ImmutableMap.of(RESPONSE_CODE_KEY, String.valueOf(response.statusCode()), STATUS_LINE_KEY, response.statusLine()));
        } catch (HttpException e) {
            throw new FileUploadException(String.format("Error when POSTing data to RadioPlayer for %", upload), e);
        } catch (InterruptedException e) {
            throw new FileUploadException("Error when sleeping during backoff", e);
        }
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
    
    @Override
    public int hashCode() {
        return Objects.hashCode(httpClient, baseUrl);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("httpClient", httpClient)
                .add("baseUrl", baseUrl)
                .toString();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof HttpsFileUploader) {
            HttpsFileUploader other = (HttpsFileUploader) that;
            return httpClient.equals(other.httpClient)
                    && baseUrl.equals(other.baseUrl);
        }
        
        return false;
    }    
}
