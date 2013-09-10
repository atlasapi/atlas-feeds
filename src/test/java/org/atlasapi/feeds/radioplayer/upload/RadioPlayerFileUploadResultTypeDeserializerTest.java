package org.atlasapi.feeds.radioplayer.upload;

import static org.junit.Assert.*;

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

    private final RadioPlayerFileUploadResultTypeDeserializer deserializer = new RadioPlayerFileUploadResultTypeDeserializer();
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(FileUploadResultType.class, deserializer)
            .create();
    
    
    @Test
    public void testReturnsSuccessIfAllSucceed() throws IOException {
        String json = getFile("all_successful.json");
        FileUploadResultType type = gson.fromJson(json, FileUploadResultType.class);
        
        assertEquals(FileUploadResultType.SUCCESS, type);
    }
    
    @Test
    public void testReturnsFailureForAllFailureModes() throws IOException {
        String json = getFile("all_failed_different_failure_modes.json");
        FileUploadResultType type = gson.fromJson(json, FileUploadResultType.class);
        
        assertEquals(FileUploadResultType.FAILURE, type);
    }
    
    @Test
    public void testReturnsFailureIfAllResultsAreFailures() throws IOException {
        String json = getFile("all_failed.json");
        FileUploadResultType type = gson.fromJson(json, FileUploadResultType.class);
        
        assertEquals(FileUploadResultType.FAILURE, type);
    }
    
    @Test
    public void testReturnsUnknownIfNoResultsKnown() throws IOException {
        String json = getFile("all_unknown.json");
        FileUploadResultType type = gson.fromJson(json, FileUploadResultType.class);
        
        assertEquals(FileUploadResultType.UNKNOWN, type);
    }
    
    @Test
    public void testReturnsFailureEvenIfSomeResultsUnknown() throws IOException {
        String json = getFile("one_failure_one_unknown.json");
        FileUploadResultType type = gson.fromJson(json, FileUploadResultType.class);
        
        assertEquals(FileUploadResultType.FAILURE, type);
    }
    
    @Test
    public void testReturnsFailureIfOneFailureDespiteOtherResults() throws IOException {
        String json = getFile("one_success_one_failure_one_unknown.json");
        FileUploadResultType type = gson.fromJson(json, FileUploadResultType.class);
        
        assertEquals(FileUploadResultType.FAILURE, type);
    }
    
    @Test
    public void testReturnsFailureIfOneFailure() throws IOException {
        String json = getFile("one_success_one_failure.json");
        FileUploadResultType type = gson.fromJson(json, FileUploadResultType.class);
        
        assertEquals(FileUploadResultType.FAILURE, type);
    }
    
    @Test
    public void testReturnsUnknownIfOneResultNotKnown() throws IOException {
        String json = getFile("one_success_one_unknown.json");
        FileUploadResultType type = gson.fromJson(json, FileUploadResultType.class);
        
        assertEquals(FileUploadResultType.UNKNOWN, type);
    }

    private String getFile(String fileName) throws IOException {
        URL testFile = Resources.getResource(getClass(), fileName);
        InputStream stream = Resources.newInputStreamSupplier(testFile).getInput();
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, Charsets.UTF_8.displayName());
        return writer.toString();
    }

}
