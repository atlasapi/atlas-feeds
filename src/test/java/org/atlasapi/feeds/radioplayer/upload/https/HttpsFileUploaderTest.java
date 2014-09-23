package org.atlasapi.feeds.radioplayer.upload.https;

import static org.atlasapi.feeds.radioplayer.upload.https.HttpsFileUploader.ERROR_MESSAGE_KEY;
import static org.atlasapi.feeds.radioplayer.upload.https.HttpsFileUploader.RESPONSE_CODE_KEY;
import static org.atlasapi.feeds.radioplayer.upload.https.HttpsFileUploader.RETRIES_KEY;
import static org.atlasapi.feeds.radioplayer.upload.https.HttpsFileUploader.STATUS_LINE_KEY;
import static org.atlasapi.feeds.radioplayer.upload.https.HttpsFileUploader.TRANSACTION_ID_KEY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;

import org.apache.commons.httpclient.HttpStatus;
import org.atlasapi.feeds.radioplayer.upload.https.HttpsFileUploader;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.BytesPayload;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class HttpsFileUploaderTest {
    
    private Clock clock = new TimeMachine(DateTime.now());
    private String baseUrl = "upload";
    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private final HttpsFileUploader fileUploader = new HttpsFileUploader(httpClient, baseUrl, clock, 10);

    @Test
    public void testReturnsSuccessUpon202Response() throws Exception {
        FileUpload file = new FileUpload("20140707_300_PI.xml", new byte[0]);
        String transactionId = "1234567890";
        Mockito.when(httpClient.post("upload/pi/", new BytesPayload(file.getFileData()))).thenReturn(getSuccessfulResponse(transactionId));
        
        UploadAttempt result = fileUploader.upload(file);
        
        assertEquals(FileUploadResultType.SUCCESS, result.uploadResult());
        assertEquals(clock.now(), result.uploadTime());
        assertEquals(transactionId, result.uploadDetails().get(TRANSACTION_ID_KEY));
        assertEquals("0", result.uploadDetails().get(RETRIES_KEY));
    }

    @Test
    public void testRetriesUpon503Response() throws Exception {
        FileUpload file = new FileUpload("20140707_300_PI.xml", new byte[0]);
        String retryAfter = "1";
        String transactionId = "1234567890";
        Mockito.when(httpClient.post("upload/pi/", new BytesPayload(file.getFileData()))).then(getRetryResponse(Optional.of(retryAfter), 1, transactionId));
        
        UploadAttempt result = fileUploader.upload(file);
        
        Mockito.verify(httpClient, times(2)).post("upload/pi/", new BytesPayload(file.getFileData()));
        
        assertEquals(FileUploadResultType.SUCCESS, result.uploadResult());
        assertEquals(clock.now(), result.uploadTime());
        assertEquals(transactionId, result.uploadDetails().get(TRANSACTION_ID_KEY));
        assertEquals("1", result.uploadDetails().get(RETRIES_KEY));
    }

    @Test
    public void testRetriesUpon503WithNoTimeoutSpecified() throws Exception {
        FileUpload file = new FileUpload("20140707_300_PI.xml", new byte[0]);
        String transactionId = "1234567890";
        Mockito.when(httpClient.post("upload/pi/", new BytesPayload(file.getFileData()))).then(getRetryResponse(Optional.<String>absent(), 1, transactionId));
        
        UploadAttempt result = fileUploader.upload(file);
        
        Mockito.verify(httpClient, times(2)).post("upload/pi/", new BytesPayload(file.getFileData()));
        
        assertEquals(FileUploadResultType.SUCCESS, result.uploadResult());
        assertEquals(clock.now(), result.uploadTime());
        assertEquals(transactionId, result.uploadDetails().get(TRANSACTION_ID_KEY));
        assertEquals("1", result.uploadDetails().get(RETRIES_KEY));
    }

    @Test
    public void testRepeatedRetriesUpon503Response() throws Exception {
        FileUpload file = new FileUpload("20140707_300_PI.xml", new byte[0]);
        String retryAfter = "1";
        String transactionId = "1234567890";
        Mockito.when(httpClient.post("upload/pi/", new BytesPayload(file.getFileData()))).then(getRetryResponse(Optional.of(retryAfter), 5, transactionId));
        
        UploadAttempt result = fileUploader.upload(file);
        
        Mockito.verify(httpClient, times(6)).post("upload/pi/", new BytesPayload(file.getFileData()));
        
        assertEquals(FileUploadResultType.SUCCESS, result.uploadResult());
        assertEquals(clock.now(), result.uploadTime());
        assertEquals(transactionId, result.uploadDetails().get(TRANSACTION_ID_KEY));
        assertEquals("5", result.uploadDetails().get(RETRIES_KEY));
    }

    @Test
    public void testRepeatedRetriesFailsEventually() throws Exception {
        FileUpload file = new FileUpload("20140707_300_PI.xml", new byte[0]);
        String retryAfter = "1";
        String transactionId = "1234567890";
        Mockito.when(httpClient.post("upload/pi/", new BytesPayload(file.getFileData()))).then(getRetryResponse(Optional.of(retryAfter), 15, transactionId));
        
        UploadAttempt result = fileUploader.upload(file);
        
        Mockito.verify(httpClient, times(12)).post("upload/pi/", new BytesPayload(file.getFileData()));
        
        assertEquals(FileUploadResultType.FAILURE, result.uploadResult());
        assertEquals(clock.now(), result.uploadTime());
        assertEquals("503", result.uploadDetails().get(RESPONSE_CODE_KEY));
        assertEquals("11", result.uploadDetails().get(RETRIES_KEY));
        assertEquals("Max Retries exceeded", result.uploadDetails().get(ERROR_MESSAGE_KEY));
    }

    @Test
    public void testReturnsFailureUponNon202Or503Response() throws Exception {
        FileUpload file = new FileUpload("20140707_300_PI.xml", new byte[0]);
        String message = "url not found";
        Mockito.when(httpClient.post("upload/pi/", new BytesPayload(file.getFileData()))).thenReturn(getFailureResponse(message));
        
        UploadAttempt result = fileUploader.upload(file);
        
        assertEquals(FileUploadResultType.FAILURE, result.uploadResult());
        assertEquals(clock.now(), result.uploadTime());
        assertEquals("404", result.uploadDetails().get(RESPONSE_CODE_KEY));
        assertEquals(message, result.uploadDetails().get(STATUS_LINE_KEY));
        assertEquals("0", result.uploadDetails().get(RETRIES_KEY));
    }

    private HttpResponse getFailureResponse(String statusLine) {
        return new HttpResponse("", HttpStatus.SC_NOT_FOUND, statusLine);
    }

    private Answer<HttpResponse> getRetryResponse(final Optional<String> retryAfter, final int tries, final String transactionId) {
        return new Answer<HttpResponse>() {
            private int count = 0;

            @Override
            public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                if (++count > tries) {
                    return getSuccessfulResponse(transactionId);
                }
                return getRetryResponse(retryAfter); 
            }
        };
        
    }
    
    private HttpResponse getRetryResponse(Optional<String> retryAfter) {
        if (retryAfter.isPresent()) {
            return new HttpResponse("", HttpStatus.SC_SERVICE_UNAVAILABLE, "", ImmutableMap.of("Retry-After", retryAfter.get()));
        } else {
            return new HttpResponse("", HttpStatus.SC_SERVICE_UNAVAILABLE, "");
        }
    }

    private HttpResponse getSuccessfulResponse(String transactionId) {
        return new HttpResponse("", HttpStatus.SC_ACCEPTED, "", ImmutableMap.of("Location", transactionId));
    }

}
