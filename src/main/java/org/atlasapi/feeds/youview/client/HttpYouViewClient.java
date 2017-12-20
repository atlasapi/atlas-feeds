package org.atlasapi.feeds.youview.client;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.tasks.Payload;

import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.url.QueryStringParameters;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.common.base.Predicate;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.client.YouViewResult.failure;
import static org.atlasapi.feeds.youview.client.YouViewResult.success;


public class HttpYouViewClient implements YouViewClient {

    private static final String TRANSACTION_URL_STEM = "/transaction";
    private static final String DELETION_URL_SUFFIX = "/fragment";
    private static final Duration SLEEP_DURATION = Duration.standardMinutes(1);

    private static final Logger log = LoggerFactory.getLogger(HttpYouViewClient.class);
    
    private final HttpRequestFactory httpRequestFactory;
    private final String urlBase;
    private final Clock clock;
    private int deleteRetryCount = 0;
    private int uploadRetryCount = 0;
    
    public HttpYouViewClient(
            HttpRequestFactory httpRequestFactory,
            String urlBase,
            Clock clock
    ) {
        this.httpRequestFactory = checkNotNull(httpRequestFactory);
        this.urlBase = checkNotNull(urlBase);
        this.clock = checkNotNull(clock);
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
                if (deleteRetryCount > 1) {
                    log.warn("YouView delete retry count - {}", deleteRetryCount);
                }

                try {
                    HttpRequest deleteRequest = httpRequestFactory.buildDeleteRequest(
                            new GenericUrl(queryUrl));

                    return deleteRequest.execute();
                } catch (IOException e) {
                    log.error("Exception executing YV request", e);
                    throw e;
                }
            }
        };

        try {
            HttpResponse response = retryer.call(deleteCall);
            deleteRetryCount = 0;
            if (response.getStatusCode() == HttpServletResponse.SC_ACCEPTED) {
                HttpHeaders headers = response.getHeaders();
                String transactionUrl = parseIdFrom(headers.getLocation());
                log.trace("Delete successful. Transaction url: " + transactionUrl);
                return success(transactionUrl, clock.now(), response.getStatusCode());
            } else {
                try {
                    return failure(response.parseAsString(), clock.now(), response.getStatusCode());
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
        } catch (ExecutionException | RetryException e) {
            throw new YouViewClientException("Error deleting id " + elementId, e);
        }
    }

    public static Retryer<HttpResponse> getHttpRequestRetryer() {
        Predicate<HttpResponse> responseCodeIsEqualOrHigherThan500 = new Predicate<HttpResponse>() {
            public boolean apply(HttpResponse response) {
                if(response.getStatusCode() >= 500 ){
                    log.error("@@@ Uploading to YV responded with an error: "+ response.getStatusCode()+ " - " + response.getStatusMessage());
                    try {
                        log.error(response.parseAsString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        };

        return RetryerBuilder.<HttpResponse>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withWaitStrategy(WaitStrategies.exponentialWait(
                        100,
                        SLEEP_DURATION.getMillis(),
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
    public YouViewResult upload(final Payload payload) {
        final String queryUrl = urlBase + TRANSACTION_URL_STEM;
        final String payloadString = payload.payload();
        log.trace("POSTing YouView output xml to {}", queryUrl);
        Retryer<HttpResponse> retryer = getHttpRequestRetryer();

        Callable<HttpResponse> uploadCall = new Callable<HttpResponse>() {
            @Override
            public HttpResponse call() throws Exception {
                uploadRetryCount++;
                if (uploadRetryCount > 1) {
                    log.warn("YouView upload retry count - {}", uploadRetryCount);
                }

                try {
                    HttpRequest postRequest = httpRequestFactory.buildPostRequest(
                            new GenericUrl(queryUrl),
                            new ByteArrayContent(
                                    MimeType.APPLICATION_XML.toString(),
                                    payloadString.getBytes("UTF-8")
                            )
                    );

                    return postRequest.execute();
                } catch (IOException e) {
                    log.error("Exception executing YV request", e);
                    throw e;
                }
            }
        };

        try {
            HttpResponse response = retryer.call(uploadCall);
            uploadRetryCount = 0;
            if (response.getStatusCode() == HttpServletResponse.SC_ACCEPTED) {
                HttpHeaders headers = response.getHeaders();
                String transactionUrl = parseIdFrom(headers.getLocation());
                log.trace("Upload successful. Transaction url: " + transactionUrl);
                return success(transactionUrl, clock.now(), response.getStatusCode());
            } else {
                try {
                    return failure(response.parseAsString(), clock.now(), response.getStatusCode());
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
        } catch (ExecutionException | RetryException e){
            throw new YouViewClientException("Error uploading " + payload, e);
        }
    }

    @Override
    public YouViewResult checkRemoteStatus(String transactionId) {
        try {
            GenericUrl txUrl = new GenericUrl(buildTransactionUrl(transactionId));
            HttpRequest getRequest = httpRequestFactory.buildGetRequest(txUrl);
            HttpResponse response = getRequest.execute();

            String bodyStr = response.parseAsString();
            if (response.getStatusCode() == HttpServletResponse.SC_OK) {
                return success(bodyStr, clock.now(), response.getStatusCode());
            } else {
                return failure(bodyStr, clock.now(), response.getStatusCode());
            }

        } catch (Exception e) {
            throw new YouViewClientException("error polling status for " + transactionId, e);
        }
    }

    private String parseIdFrom(String transactionUrl) {
        return transactionUrl.replace(urlBase + TRANSACTION_URL_STEM + "/", "");
    }

    private String buildTransactionUrl(String id) {
        return urlBase + TRANSACTION_URL_STEM + "/" + id;
    }
}
