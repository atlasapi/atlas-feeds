package org.atlasapi.feeds.youview.client;

import org.atlasapi.feeds.tasks.Payload;

import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class HttpYouViewClientTest {
    
    @Captor
    private ArgumentCaptor<SimpleHttpRequest<YouViewResult>> requestCaptor;    
    private SimpleHttpClient httpClient = mock(SimpleHttpClient.class);
    private String baseUrl = "youviewUrl";
    private Clock clock = new TimeMachine();
    private Payload payload = createPayload();
    
    private final YouViewClient client = new HttpYouViewClient(httpClient, baseUrl, clock);
    
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testThatUploadingSuccessfullyReturnsSuccessfulResult() throws HttpException {
        String txnUrl = "transactionUrl";
        HttpResponse response = createSuccessfulResponseWithTransaction(txnUrl);
        when(httpClient.post(eq(baseUrl + "/transaction"), any(StringPayload.class))).thenReturn(response);
        
        YouViewResult result = client.upload(payload);
        
        Mockito.verify(httpClient).post(Mockito.eq(baseUrl + "/transaction"), any(StringPayload.class));
        
        
        assertTrue("202 response should yield successful result", result.isSuccess());
        assertEquals(txnUrl, result.result());
    }

    @Test
    public void testThatAFailedUploadReturnsFailedResult() throws HttpException {
        String error = "something went wrong";
        HttpResponse response = createResponse(error, 400);
        when(httpClient.post(eq(baseUrl + "/transaction"), any(StringPayload.class))).thenReturn(response);
        
        YouViewResult result = client.upload(payload);
        
        Mockito.verify(httpClient).post(Mockito.eq(baseUrl + "/transaction"), any(StringPayload.class));
        
        
        assertFalse("Non-202 response should yield failed result", result.isSuccess());
        assertEquals(error, result.result());
    }

    @Test
    public void testThatDeletingSuccessfullyReturnsSuccessfulResult() throws HttpException {
        String txnUrl = "transactionUrl";
        HttpResponse response = createSuccessfulResponseWithTransaction(txnUrl);
        String deletedId = "anId";
        when(httpClient.delete(eq(baseUrl + "/fragment?id=" + deletedId))).thenReturn(response);
        
        YouViewResult result = client.delete(deletedId);
        
        Mockito.verify(httpClient).delete(Mockito.eq(baseUrl + "/fragment?id=" + deletedId));
        
        assertTrue("202 response should yield successful result", result.isSuccess());
        assertEquals(txnUrl, result.result());
    }

    @Test
    public void testThatAFailedDeleteReturnsFailedResult() throws HttpException {
        String error = "something went wrong";
        HttpResponse response = createResponse(error, 400);
        String deletedId = "anId";
        when(httpClient.delete(eq(baseUrl + "/fragment?id=" + deletedId))).thenReturn(response);
        
        YouViewResult result = client.delete(deletedId);
        
        Mockito.verify(httpClient).delete(Mockito.eq(baseUrl + "/fragment?id=" + deletedId));
        
        assertFalse("Non-202 response should yield failed result", result.isSuccess());
        assertEquals(error, result.result());
    }

    @Test
    public void testThatCheckingStatusSuccessfullyReturnsSuccessfulResult() throws Exception {
        String report = "status report";
        when(httpClient.get(Mockito.<SimpleHttpRequest<YouViewResult>>any())).thenReturn(YouViewResult.success(report, clock.now(), 202));
        
        YouViewResult result = client.checkRemoteStatus("txnId");
        
        Mockito.verify(httpClient).get(requestCaptor.capture());
        
        
        assertEquals(baseUrl + "/transaction/" + "txnId", requestCaptor.getValue().getUrl());
        assertTrue("202 response should yield successful result", result.isSuccess());
        assertEquals(report, result.result());
    }

    @Test
    public void testThatAFailedStatusCheckReturnsFailedResult() throws Exception {
        String error = "something went wrong";
        when(httpClient.get(Mockito.<SimpleHttpRequest<YouViewResult>>any())).thenReturn(YouViewResult.failure(error, clock.now(), 503));
        
        YouViewResult result = client.checkRemoteStatus("txnId");
        
        Mockito.verify(httpClient).get(requestCaptor.capture());
        
        assertEquals(baseUrl + "/transaction/" + "txnId", requestCaptor.getValue().getUrl());
        assertFalse("Non-202 response should yield failed result", result.isSuccess());
        assertEquals(error, result.result());
    }

    @Test
    public void testRetryLogicWhenYouViewHttpResponseCodeIs500AndAfterThat200()
            throws HttpException {
        String txnUrl = "transactionUrl";
        HttpResponse unsuccessfulResponse = createUnsuccessfulResponseWithTransaction(txnUrl);
        HttpResponse successfulResponse = createSuccessfulResponseWithTransaction(txnUrl);
        when(httpClient.post(eq(baseUrl + "/transaction"), any(StringPayload.class)))
                .thenReturn(unsuccessfulResponse, successfulResponse);

        YouViewResult result = client.upload(payload);

        assertTrue("202 response should yield successful result", result.isSuccess());
        assertEquals(txnUrl, result.result());
    }

    private HttpResponse createUnsuccessfulResponseWithTransaction(String transactionUrl) {
        return new HttpResponse("", 500, "INTERNAL SERVER ERROR", ImmutableMap.of("Location", transactionUrl));
    }

    private HttpResponse createSuccessfulResponseWithTransaction(String transactionUrl) {
        return new HttpResponse("", 202, "SUCCESSED", ImmutableMap.of("Location", transactionUrl));
    }

    private HttpResponse createResponse(String body, int statusCode) {
        return new HttpResponse(body, statusCode);
    }
    
    private Payload createPayload() {
        return new Payload("payload", clock.now());
    }
}
