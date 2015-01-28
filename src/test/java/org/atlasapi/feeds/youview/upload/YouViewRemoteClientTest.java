package org.atlasapi.feeds.youview.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.atlasapi.feeds.youview.client.YouViewResult;
import org.atlasapi.feeds.youview.upload.HttpYouViewRemoteClient;
import org.atlasapi.feeds.youview.upload.YouViewRemoteClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tva.metadata._2010.ObjectFactory;
import tva.metadata._2010.TVAMainType;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.metabroadcast.common.http.StringPayload;


public class YouViewRemoteClientTest {

    @Captor
    private ArgumentCaptor<SimpleHttpRequest<YouViewResult>> requestCaptor;    
    private SimpleHttpClient httpClient = mock(SimpleHttpClient.class);
    private String baseUrl = "youviewUrl";
    private final YouViewRemoteClient client;
    
    public YouViewRemoteClientTest() throws JAXBException {
        this.client = new HttpYouViewRemoteClient(httpClient, baseUrl);
    }
    
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testThatUploadingSuccessfullyReturnsSuccessfulResult() throws HttpException {
        String txnUrl = "transactionUrl";
        HttpResponse response = createSuccessfulResponseWithTransaction(txnUrl);
        when(httpClient.post(eq(baseUrl + "/transaction"), any(StringPayload.class))).thenReturn(response);
        
        YouViewResult result = client.upload(createTVAElem());
        
        Mockito.verify(httpClient).post(Mockito.eq(baseUrl + "/transaction"), any(StringPayload.class));
        
        
        assertTrue("202 response should yield successful result", result.isSuccess());
        assertEquals(txnUrl, result.result());
    }

    @Test
    public void testThatAFailedUploadReturnsFailedResult() throws HttpException {
        String error = "something went wrong";
        HttpResponse response = createResponse(error, 400);
        when(httpClient.post(eq(baseUrl + "/transaction"), any(StringPayload.class))).thenReturn(response);
        
        YouViewResult result = client.upload(createTVAElem());
        
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
        
        YouViewResult result = client.sendDeleteFor(deletedId);
        
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
        
        YouViewResult result = client.sendDeleteFor(deletedId);
        
        Mockito.verify(httpClient).delete(Mockito.eq(baseUrl + "/fragment?id=" + deletedId));
        
        assertFalse("Non-202 response should yield failed result", result.isSuccess());
        assertEquals(error, result.result());
    }

    @Test
    public void testThatCheckingStatusSuccessfullyReturnsSuccessfulResult() throws Exception {
        String report = "status report";
        when(httpClient.get(Mockito.<SimpleHttpRequest<YouViewResult>>any())).thenReturn(YouViewResult.success(report));
        
        YouViewResult result = client.checkRemoteStatusOf("txnId");
        
        Mockito.verify(httpClient).get(requestCaptor.capture());
        
        
        assertEquals(baseUrl + "/transaction/" + "txnId", requestCaptor.getValue().getUrl());
        assertTrue("202 response should yield successful result", result.isSuccess());
        assertEquals(report, result.result());
    }

    @Test
    public void testThatAFailedStatusCheckReturnsFailedResult() throws Exception {
        String error = "something went wrong";
        when(httpClient.get(Mockito.<SimpleHttpRequest<YouViewResult>>any())).thenReturn(YouViewResult.failure(error));
        
        YouViewResult result = client.checkRemoteStatusOf("txnId");
        
        Mockito.verify(httpClient).get(requestCaptor.capture());
        
        assertEquals(baseUrl + "/transaction/" + "txnId", requestCaptor.getValue().getUrl());
        assertFalse("Non-202 response should yield failed result", result.isSuccess());
        assertEquals(error, result.result());
    }

    private HttpResponse createSuccessfulResponseWithTransaction(String transactionUrl) {
        return new HttpResponse("", 202, "SUCCESSED", ImmutableMap.of("Location", transactionUrl));
    }

    private HttpResponse createResponse(String body, int statusCode) {
        return new HttpResponse(body, statusCode);
    }
    
    private JAXBElement<TVAMainType> createTVAElem() {
        ObjectFactory factory = new ObjectFactory();
        TVAMainType tvaMain = factory.createTVAMainType();
        return factory.createTVAMain(tvaMain);
    }
}
