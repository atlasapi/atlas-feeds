package org.atlasapi.feeds.radioplayer.upload.https;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckException;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckResult;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckService;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckTask;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;


public class HttpsRemoteCheckService implements RemoteCheckService {

    private final SimpleHttpClient httpClient;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(RemoteCheckResult.class, new RadioPlayerHttpsRemoteResultDeserializer())
            .create();
    
    public HttpsRemoteCheckService(SimpleHttpClient httpClient) {
        this.httpClient = checkNotNull(httpClient);
    }

    @Override
    public RemoteCheckResult check(final RemoteCheckTask task) throws RemoteCheckException {
        Optional<String> transactionId = task.getParameter(HttpsFileUploader.TRANSACTION_ID_KEY);
        if (!transactionId.isPresent()) {
            return RemoteCheckResult.failure(String.format("no transaction id for task %s", task.toString()));
        }
        
        try {
            return httpClient.get(SimpleHttpRequest.httpRequestFrom(transactionId.get(), new HttpResponseTransformer<RemoteCheckResult>() {
                @Override
                public RemoteCheckResult transform(HttpResponsePrologue prologue, InputStream body)
                        throws HttpException, Exception {
                    if (prologue.statusCode() == HttpStatusCode.NOT_FOUND.code()) {
                        return RemoteCheckResult.failure(String.format("404 - Transaction Id not found in RadioPlayer system for task %s", task.toString()));
                    }
                    return gson.fromJson(new InputStreamReader(body, Charsets.UTF_8), RemoteCheckResult.class);
                }
            }));
        } catch (Exception e) {
            throw new RemoteCheckException(String.format("Error while checking remote processing result for task %s", task.toString()), e);
        }
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
        if (that instanceof HttpsRemoteCheckService) {
            HttpsRemoteCheckService other = (HttpsRemoteCheckService) that;
            return httpClient.equals(other.httpClient);
        }
        
        return false;
    }
}
