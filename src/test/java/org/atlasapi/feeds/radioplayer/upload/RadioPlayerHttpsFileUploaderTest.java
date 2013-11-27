package org.atlasapi.feeds.radioplayer.upload;

import static org.junit.Assert.*;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploaderResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.BytesPayload;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;


public class RadioPlayerHttpsFileUploaderTest {

    private SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
    private String radioPlayerUrl = "radioPlayerUploadUrl";
    private final RadioPlayerHttpsFileUploader uploader = new RadioPlayerHttpsFileUploader(httpClient, radioPlayerUrl );
    
    @Test
    public void testSuccessfulUploadReturnsUnknownStatus() throws Exception {
        
        byte[] fileData = new byte[0];
        FileUpload file = new FileUpload("20130905_352_PI.xml", fileData);
        String urlToPoll = "urlToPoll";
        HttpResponse response = new HttpResponse("response", 202, "statusLine", ImmutableMap.of("Location", urlToPoll));
        Mockito.when(httpClient.post(radioPlayerUrl + "/pi/", new BytesPayload(fileData))).thenReturn(response);
        
        FileUploaderResult result = uploader.upload(file);
        
        assertEquals(FileUploadResultType.SUCCESS, result.getStatus());
        
        assertEquals(urlToPoll, result.getTransactionId().get());
        assertFalse(result.getMessage().isPresent());
    }

    @Test
    public void testFailedUploadReturnsFailureStatus() throws Exception {
        
        byte[] fileData = new byte[0];
        FileUpload file = new FileUpload("20130905_352_PI.xml", fileData);
        HttpResponse response = new HttpResponse("not found", 404);
        Mockito.when(httpClient.post(radioPlayerUrl + "/pi/", new BytesPayload(fileData))).thenReturn(response);
        
        FileUploaderResult result = uploader.upload(file);
        
        assertEquals(FileUploadResultType.FAILURE, result.getStatus());
        
        assertFalse(result.getTransactionId().isPresent());
        assertFalse(result.getMessage().isPresent());
    }

    @Test
    public void testTimedOutUploadReturnsFailureWithTimeToRetry() throws Exception {
        
        byte[] fileData = new byte[0];
        FileUpload file = new FileUpload("20130905_352_PI.xml", fileData);
        String retryAfter = "1000";
        HttpResponse response = new HttpResponse("retry after", 503, "statusLine", ImmutableMap.of("RetryAfter", retryAfter ));
        Mockito.when(httpClient.post(radioPlayerUrl + "/pi/", new BytesPayload(fileData))).thenReturn(response);
        
        FileUploaderResult result = uploader.upload(file);
        
        assertEquals(FileUploadResultType.FAILURE, result.getStatus());

        assertFalse(result.getTransactionId().isPresent());
        assertEquals(retryAfter, result.getMessage().get());
    }
}
