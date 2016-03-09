package org.atlasapi.feeds.youview.client;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tasks.Payload;

import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.url.QueryStringParameters;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicate;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.client.YouViewResult.failure;
import static org.atlasapi.feeds.youview.client.YouViewResult.success;


public class HttpYouViewClient implements YouViewClient {

    private static final String TRANSACTION_URL_STEM = "/transaction";
    private static final String DELETION_URL_SUFFIX = "/fragment";
    private static final Duration SLEEP_DURATION = Duration.ofMillis(60000);
    
    private final Logger log = LoggerFactory.getLogger(HttpYouViewClient.class);
    
    private final SimpleHttpClient httpClient;
    private final String urlBase;
    private final Clock clock;
    private final YouViewResultTransformer resultTransformer;
    private int deleteRetryCount = 0;
    private int uploadRetryCount = 0;
    
    public HttpYouViewClient(SimpleHttpClient httpClient, String urlBase, Clock clock) {
        this.httpClient = checkNotNull(httpClient);
        this.urlBase = checkNotNull(urlBase);
        this.clock = checkNotNull(clock);
        this.resultTransformer = new YouViewResultTransformer(clock);
    }

    @Override
    public YouViewResult delete(String elementId) {
        final String queryUrl = buildDeleteQuery(elementId);
        log.trace("Sending Delete request: {}", queryUrl);
        Retryer<HttpResponse> retryer = getHttpRequestRetryer();

        Callable<HttpResponse> deleteCall = new Callable<HttpResponse>() {
            @Override
            public HttpResponse call() throws Exception {
                deleteRetryCount++;
                log.warn("YouView delete retry count - {}", deleteRetryCount);
                return httpClient.delete(queryUrl);
            }
        };

        try {
            HttpResponse response = retryer.call(deleteCall);
            deleteRetryCount = 0;
            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                String transactionUrl = parseIdFrom(response.header("Location"));
                log.trace("Delete successful. Transaction url: " + transactionUrl);
                return success(transactionUrl, clock.now(), response.statusCode());
            } else {
                return failure(response.body(), clock.now(), response.statusCode());
            }
        } catch (ExecutionException e) {
            throw new YouViewClientException("Error deleting id " + elementId, e);
        } catch (RetryException e) {
            throw new YouViewClientException("Error deleting id " + elementId, e);
        }
    }

    public static Retryer<HttpResponse> getHttpRequestRetryer() {
        Predicate<HttpResponse> responseCodeIsEqualOrHigherThan500 = new Predicate<HttpResponse>() {
            public boolean apply(HttpResponse response) {
                return response.statusCode() >= 500;
            }
        };

        return RetryerBuilder.<HttpResponse>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withWaitStrategy(WaitStrategies.exponentialWait(100, SLEEP_DURATION.toMillis(),
                        TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.neverStop())
                .retryIfResult(responseCodeIsEqualOrHigherThan500)
                .build();
    }

    private String buildDeleteQuery(String remoteId) {
        QueryStringParameters qsp = new QueryStringParameters();
        qsp.add("id", remoteId);
        return urlBase + DELETION_URL_SUFFIX + "?" + qsp.toQueryString();
    }


    @Override
    public YouViewResult upload(Payload payload) {
        final String queryUrl = urlBase + TRANSACTION_URL_STEM;
        final String payloadString = payload.payload();
        log.trace("POSTing YouView output xml to {}", queryUrl);
        Retryer<HttpResponse> retryer = getHttpRequestRetryer();

        Callable<HttpResponse> uploadCall = new Callable<HttpResponse>() {
            @Override
            public HttpResponse call() throws Exception {
                uploadRetryCount++;
                log.warn("YouView upload retry count - {}", uploadRetryCount);
                return httpClient.post(queryUrl, new StringPayload(payloadString));
            }
        };

        try {
            HttpResponse response = retryer.call(uploadCall);
            uploadRetryCount = 0;
            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                String transactionUrl = parseIdFrom(response.header("Location"));
                log.trace("Upload successful. Transaction url: " + transactionUrl);
                return success(transactionUrl, clock.now(), response.statusCode());
            } else {
                return failure(response.body(), clock.now(), response.statusCode());
            }
        } catch (ExecutionException e) {
            throw new YouViewClientException("Error uploading " + payload, e);
        } catch (RetryException e) {
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
        public YouViewResult transform(HttpResponsePrologue response, InputStream body)
                throws HttpException, Exception {
            String bodyStr = IOUtils.toString(body, "UTF-8");
            if (response.statusCode() == HttpServletResponse.SC_OK) {
                return success(bodyStr, clock.now(), response.statusCode());
            }
            return failure(bodyStr, clock.now(), response.statusCode());
        }
    }

    private String parseIdFrom(String transactionUrl) {
        return transactionUrl.replace(urlBase + TRANSACTION_URL_STEM + "/", "");
    }

    private String buildTransactionUrl(String id) {
        return urlBase + TRANSACTION_URL_STEM + "/" + id;
    }
}
