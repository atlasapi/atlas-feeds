package org.atlasapi.feeds.radioplayer.upload.https;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckService;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckServiceProvider;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.metabroadcast.common.http.SimpleHttpClient;


public class HttpsRemoteCheckServiceProvider implements RemoteCheckServiceProvider {

    private final SimpleHttpClient httpClient;
    
    public HttpsRemoteCheckServiceProvider(SimpleHttpClient httpClient) {
        this.httpClient = checkNotNull(httpClient);
    }
    
    @Override
    public RemoteCheckService get(DateTime uploadTime, FileType type) {
        return new HttpsRemoteCheckService(httpClient);
    }

    @Override
    public UploadService remoteService() {
        return UploadService.HTTPS;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(httpClient);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("httpClient", httpClient)
                .toString();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof HttpsRemoteCheckServiceProvider) {
            HttpsRemoteCheckServiceProvider other = (HttpsRemoteCheckServiceProvider) that;
            return httpClient.equals(other.httpClient);
        }
        
        return false;
    }
}
