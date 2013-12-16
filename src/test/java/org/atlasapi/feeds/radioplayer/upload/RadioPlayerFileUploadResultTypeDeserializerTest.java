package org.atlasapi.feeds.radioplayer.upload;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class RadioPlayerFileUploadResultTypeDeserializerTest {

    private final RadioPlayerHttpsRemoteResultDeserializer deserializer = new RadioPlayerHttpsRemoteResultDeserializer();
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(RadioPlayerHttpsRemoteResult.class, deserializer)
            .create();
    
    
    @Test
    public void testReturnsSuccessIfAllSucceed() throws IOException {
        String json = getFile("all_successful.json");
        RadioPlayerHttpsRemoteResult result = gson.fromJson(json, RadioPlayerHttpsRemoteResult.class);
        
        assertEquals(FileUploadResultType.SUCCESS, result.getResultType());
    }
    
    @Test
    public void testReturnsFailureForAllFailureModes() throws IOException {
        String json = getFile("all_failed_different_failure_modes.json");
        RadioPlayerHttpsRemoteResult result = gson.fromJson(json, RadioPlayerHttpsRemoteResult.class);
        
        assertEquals(FileUploadResultType.FAILURE, result.getResultType());
    }
    
    @Test
    public void testReturnsFailureIfAllResultsAreFailures() throws IOException {
        String json = getFile("all_failed.json");
        RadioPlayerHttpsRemoteResult result = gson.fromJson(json, RadioPlayerHttpsRemoteResult.class);
        
        assertEquals(FileUploadResultType.FAILURE, result.getResultType());
    }
    
    @Test
    public void testReturnsUnknownIfNoResultsKnown() throws IOException {
        String json = getFile("all_unknown.json");
        RadioPlayerHttpsRemoteResult result = gson.fromJson(json, RadioPlayerHttpsRemoteResult.class);
        
        assertEquals(FileUploadResultType.UNKNOWN, result.getResultType());
    }
    
    @Test
    public void testReturnsFailureEvenIfSomeResultsUnknown() throws IOException {
        String json = getFile("one_failure_one_unknown.json");
        RadioPlayerHttpsRemoteResult result = gson.fromJson(json, RadioPlayerHttpsRemoteResult.class);
        
        assertEquals(FileUploadResultType.FAILURE, result.getResultType());
    }
    
    @Test
    public void testReturnsFailureIfOneFailureDespiteOtherResults() throws IOException {
        String json = getFile("one_success_one_failure_one_unknown.json");
        RadioPlayerHttpsRemoteResult result = gson.fromJson(json, RadioPlayerHttpsRemoteResult.class);
        
        assertEquals(FileUploadResultType.FAILURE, result.getResultType());
    }
    
    @Test
    public void testReturnsFailureIfOneFailure() throws IOException {
        String json = getFile("one_success_one_failure.json");
        RadioPlayerHttpsRemoteResult result = gson.fromJson(json, RadioPlayerHttpsRemoteResult.class);
        
        assertEquals(FileUploadResultType.FAILURE, result.getResultType());
    }
    
    @Test
    public void testReturnsUnknownIfOneResultNotKnown() throws IOException {
        String json = getFile("one_success_one_unknown.json");
        RadioPlayerHttpsRemoteResult result = gson.fromJson(json, RadioPlayerHttpsRemoteResult.class);
        
        assertEquals(FileUploadResultType.UNKNOWN, result.getResultType());
    }

    private String getFile(String fileName) throws IOException {
        URL testFile = Resources.getResource(getClass(), fileName);
        InputStream stream = Resources.newInputStreamSupplier(testFile).getInput();
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, Charsets.UTF_8.displayName());
        return writer.toString();
    }

}
