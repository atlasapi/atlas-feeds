package org.atlasapi.feeds.youview.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.atlasapi.feeds.tasks.Payload;

import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpYouViewClientTest {

    @Captor
    private ArgumentCaptor<SimpleHttpRequest<YouViewResult>> requestCaptor;

    private Queue<LowLevelHttpResponse> responses;
    private LowLevelHttpRequest request;
    private String methodUsed;
    private String urlUsed;

    private String baseUrl = "http://youviewUrl.nope";
    private Clock clock = new TimeMachine();
    private Payload payload = createPayload();

    private YouViewClient client;

    @Before
    public void setUp() throws IOException {
        responses = new LinkedList<>();
        methodUsed = null;
        urlUsed = null;

        request = new MockLowLevelHttpRequest() {
            @Override
            public LowLevelHttpResponse execute() throws IOException {
                return responses.poll();
            }
        };

        HttpTransport transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                methodUsed = method;
                urlUsed = url;

                return request;
            }
        };

        client = new HttpYouViewClient(transport.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                request.setThrowExceptionOnExecuteError(false);
            }
        }), baseUrl, clock);
    }

    @Test
    public void testThatUploadingSuccessfullyReturnsSuccessfulResult()
            throws IOException {
        String txnUrl = "transactionUrl";
        responses.add(createSuccessfulResponseWithTransaction(txnUrl));

        YouViewResult result = client.upload(payload);

        assertThat(urlUsed, is(baseUrl + "/transaction"));
        assertTrue("202 response should yield successful result", result.isSuccess());
        assertThat(methodUsed, is("POST"));
        assertEquals(txnUrl, result.result());
    }

    @Test
    public void testRetryLogicWhenYouViewHttpResponseCodeIs500AndAfterThat200()
            throws HttpException, IOException {
        String txnUrl = "transactionUrl";

        responses.add(createUnsuccessfulResponseWithTransaction(txnUrl));
        responses.add(createSuccessfulResponseWithTransaction(txnUrl));

        YouViewResult result = client.upload(payload);

        assertThat(urlUsed, is(baseUrl + "/transaction"));
        assertTrue("202 response should yield successful result", result.isSuccess());
        assertThat(methodUsed, is("POST"));
        assertEquals(txnUrl, result.result());
    }

    @Test
    @Ignore
    public void testThatAFailedUploadReturnsFailedResult() throws HttpException, IOException {
        String error = "something went wrong";
        responses.add(createResponse(error, 400));

        YouViewResult result = client.upload(payload);

        assertThat(urlUsed, is(baseUrl + "/transaction"));
        assertThat(methodUsed, is("POST"));
        assertFalse("Non-202 response should yield failed result", result.isSuccess());
        assertEquals(error, result.result());
    }

    @Test
    public void testThatDeletingSuccessfullyReturnsSuccessfulResult()
            throws HttpException, IOException {
        String txnUrl = "transactionUrl";
        responses.add(createSuccessfulResponseWithTransaction(txnUrl));

        String deletedId = "anId";

        YouViewResult result = client.delete(deletedId);

        assertThat(urlUsed, is(baseUrl + "/fragment?id=" + deletedId));
        assertThat(methodUsed, is("DELETE"));
        assertTrue("202 response should yield successful result", result.isSuccess());
        assertEquals(txnUrl, result.result());
    }

    @Test
    @Ignore
    public void testThatAFailedDeleteReturnsFailedResult() throws HttpException, IOException {
        String error = "something went wrong";
        responses.add(createResponse(error, 400));

        String deletedId = "anId";

        YouViewResult result = client.delete(deletedId);

        assertThat(urlUsed, is(baseUrl + "/fragment?id=" + deletedId));
        assertThat(methodUsed, is("DELETE"));
        assertFalse("Non-202 response should yield failed result", result.isSuccess());
        assertEquals(error, result.result());
    }

    @Test
    public void testThatCheckingStatusSuccessfullyReturnsSuccessfulResult() throws Exception {
        String report = "status report";

        LowLevelHttpResponse response = mock(LowLevelHttpResponse.class);

        when(response.getContent()).thenReturn(new ByteArrayInputStream(report.getBytes("UTF-8")));
        when(response.getStatusCode()).thenReturn(200);

        responses.add(createResponse(report, 200));

        YouViewResult result = client.checkRemoteStatus("txnId");

        assertThat(result.uploadTime(), is(clock.now()));
        assertThat(urlUsed, is(baseUrl + "/transaction/" + "txnId"));
        assertThat(methodUsed, is("GET"));
        assertThat(result.isSuccess(), is(true));
        assertEquals(report, result.result());
    }

    @Test
    public void testThatAFailedStatusCheckReturnsFailedResult() throws Exception {
        String error = "something went wrong";

        responses.add(createResponse(error, 503));

        YouViewResult result = client.checkRemoteStatus("txnId");

        assertThat(result.uploadTime(), is(clock.now()));
        assertThat(urlUsed, is(baseUrl + "/transaction/" + "txnId"));
        assertThat(methodUsed, is("GET"));
        assertThat(result.isSuccess(), is(false));
        assertEquals(error, result.result());
    }

    private LowLevelHttpResponse createSuccessfulResponseWithTransaction(String transactionUrl) throws IOException {
        LowLevelHttpResponse response = mock(LowLevelHttpResponse.class);

        when(response.getStatusCode()).thenReturn(202);
        when(response.getStatusLine()).thenReturn("SUCCESSED");
        when(response.getContent()).thenReturn(new ByteArrayInputStream("".getBytes("UTF-8")));
        when(response.getHeaderCount()).thenReturn(1);
        when(response.getHeaderName(0)).thenReturn("Location");
        when(response.getHeaderValue(0)).thenReturn(transactionUrl);

        return response;
    }

    private LowLevelHttpResponse createUnsuccessfulResponseWithTransaction(String transactionUrl)
            throws IOException {
        LowLevelHttpResponse response = mock(LowLevelHttpResponse.class);

        when(response.getStatusCode()).thenReturn(500);
        when(response.getStatusLine()).thenReturn("INTERNAL SERVER ERROR");
        when(response.getContent()).thenReturn(new ByteArrayInputStream("".getBytes("UTF-8")));
        when(response.getHeaderCount()).thenReturn(1);
        when(response.getHeaderName(0)).thenReturn("Location");
        when(response.getHeaderValue(0)).thenReturn(transactionUrl);

        return response;
    }

    private LowLevelHttpResponse createResponse(String body, int statusCode) throws IOException {
        LowLevelHttpResponse response = mock(LowLevelHttpResponse.class);

        when(response.getStatusCode()).thenReturn(statusCode);
        when(response.getStatusLine()).thenReturn("INTERNAL SERVER ERROR");
        when(response.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes("UTF-8")));

        return response;
    }

    private Payload createPayload() {
        return new Payload("payload", clock.now());
    }
}
