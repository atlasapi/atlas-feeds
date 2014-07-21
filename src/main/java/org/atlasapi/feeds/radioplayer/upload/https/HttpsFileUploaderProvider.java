package org.atlasapi.feeds.radioplayer.upload.https;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.queue.FileUploader;
import org.atlasapi.feeds.radioplayer.upload.queue.FileUploaderProvider;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.time.Clock;


public class HttpsFileUploaderProvider implements FileUploaderProvider {
    
    private final SimpleHttpClient httpClient;
    private final String baseUrl;
    private final Clock clock;

    public HttpsFileUploaderProvider(SimpleHttpClient httpClient, String baseUrl, Clock clock) {
        this.httpClient = checkNotNull(httpClient);
        this.baseUrl = checkNotNull(baseUrl);
        this.clock = checkNotNull(clock);
    }
    
    @Override
    public FileUploader get(DateTime uploadTime, FileType type) {
        return new HttpsFileUploader(httpClient, baseUrl, clock);
    }

    @Override
    public UploadService serviceKey() {
        return UploadService.HTTPS;
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
        if (that instanceof HttpsFileUploaderProvider) {
            HttpsFileUploaderProvider other = (HttpsFileUploaderProvider) that;
            return httpClient.equals(other.httpClient)
                    && baseUrl.equals(other.baseUrl);
        }
        
        return false;
    } 
}
