package org.atlasapi.feeds.youview.tasks.processing;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.upload.YouViewResult.failure;
import static org.atlasapi.feeds.youview.upload.YouViewResult.success;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.atlasapi.feeds.youview.tasks.Payload;
import org.atlasapi.feeds.youview.upload.YouViewClientException;
import org.atlasapi.feeds.youview.upload.YouViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.url.QueryStringParameters;


public class HttpYouViewClient implements YouViewClient {

    private static final String TRANSACTION_URL_STEM = "/transaction";
    private static final String DELETION_URL_SUFFIX = "/fragment";
    
    private final Logger log = LoggerFactory.getLogger(HttpYouViewClient.class);
    
    private final SimpleHttpClient httpClient;
    private final String urlBase;
    private final Clock clock;
    private final YouViewResultTransformer resultTransformer;
    
    public HttpYouViewClient(SimpleHttpClient httpClient, String urlBase, Clock clock) {
        this.httpClient = checkNotNull(httpClient);
        this.urlBase = checkNotNull(urlBase);
        this.clock = checkNotNull(clock);
        this.resultTransformer = new YouViewResultTransformer(clock);
    }

    @Override
    public YouViewResult delete(String elementId) {
        try {
            String queryUrl = buildDeleteQuery(elementId); 
            log.trace(String.format("Sending Delete request: %s", queryUrl));
            
            HttpResponse response = httpClient.delete(queryUrl);
            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                String transactionUrl = parseIdFrom(response.header("Location"));
                log.trace("Delete successful. Transaction url: " + transactionUrl);
                return success(transactionUrl, clock.now());
            } 
            
            return failure(response.body(), clock.now());
        } catch (HttpException e) {
            throw new YouViewClientException("Error deleting id " + elementId, e);
        }
    }

    private String buildDeleteQuery(String remoteId) {
        QueryStringParameters qsp = new QueryStringParameters();
        qsp.add("id", remoteId);
        return urlBase + DELETION_URL_SUFFIX + "?" + qsp.toQueryString();
    }

    @Override
    public YouViewResult upload(Payload payload) {
        try {
            String queryUrl = urlBase + TRANSACTION_URL_STEM;
            log.trace(String.format("POSTing YouView output xml to %s", queryUrl));

            HttpResponse response = httpClient.post(queryUrl, new StringPayload(payload.payload()));

            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                String transactionUrl = parseIdFrom(response.header("Location"));
                log.trace("Upload successful. Transaction url: " + transactionUrl);
                return success(transactionUrl, clock.now());
            }

            return failure(response.body(), clock.now());
        } catch (HttpException e) {
            throw new YouViewClientException("Error uploading " + payload, e);
        }
    }

    @Override
    public YouViewResult checkRemoteStatus(String transactionId) {
        try {
            return httpClient.get(new SimpleHttpRequest<>(buildTransactionUrl(transactionId), resultTransformer));
        } catch (Exception e) {
            throw new YouViewClientException("error polling status for " + transactionId, e);
        }
    }
    
    public static final class YouViewResultTransformer implements HttpResponseTransformer<YouViewResult> {
        
        private final Clock clock;
        
        public YouViewResultTransformer(Clock clock) {
            this.clock = checkNotNull(clock);
        }
        
        @Override
        public YouViewResult transform(HttpResponsePrologue prologue, InputStream body)
                throws HttpException, Exception {
            String bodyStr = IOUtils.toString(body, "UTF-8");
            if (prologue.statusCode() == HttpServletResponse.SC_OK) {
                return success(bodyStr, clock.now());
            }
            return failure(bodyStr, clock.now());
        }
    }

    private String parseIdFrom(String transactionUrl) {
        return transactionUrl.replace(urlBase + TRANSACTION_URL_STEM + "/", "");
    }

    private String buildTransactionUrl(String id) {
        return urlBase + TRANSACTION_URL_STEM + "/" + id;
    }
}
