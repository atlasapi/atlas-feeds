package org.atlasapi.feeds.radioplayer.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;

import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.atlasapi.feeds.xml.XMLValidator;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class ValidatingFTPFileUploadTest {

    @Test
    @Ignore
    public void testCallsDelegateOnSuccess() throws Exception {
        
        XMLValidator validator = XMLValidator.forSchemas(ImmutableSet.<InputStream>of(/*
                Resources.getResource("org/atlasapi/feeds/radioplayer/epgSI_10.xsd").openStream(),
                Resources.getResource("org/atlasapi/feeds/radioplayer/epgSchedule_10.xsd").openStream()
        */));
        
        Mockery context = new Mockery();
        
        final FTPFileUploader delegate = context.mock(FTPFileUploader.class);
        
        String filename = "test";
        final FTPUploadResult successfulUpload = FTPUploadResult.successfulUpload(filename);
        byte[] fileData = bytesFromResource("org/atlasapi/feeds/radioplayer/basicPIFeedTest.xml");
        
        ValidatingFTPFileUploader uploadTask = new ValidatingFTPFileUploader(validator, delegate);
        
        context.checking(new Expectations(){{
            one(delegate).upload(with(any(FTPUpload.class))); 
                will(returnValue(successfulUpload));
        }});
        
        FTPUploadResult result = uploadTask.upload(new FTPUpload(filename , fileData));
        
        assertThat(result, is(equalTo(successfulUpload)));
    }
    
    private byte[] bytesFromResource(String file) throws IOException {
        return Resources.toByteArray(Resources.getResource(file));
    }

    @Test
    @Ignore
    public void testDoesntCallDelegateOnFailure() throws Exception {
        XMLValidator validator = XMLValidator.forSchemas(ImmutableSet.<InputStream>of(/*
                Resources.getResource("org/atlasapi/feeds/radioplayer/epgSI_10.xsd").openStream(),
                Resources.getResource("org/atlasapi/feeds/radioplayer/epgSchedule_10.xsd").openStream()*/
        ));
        
        Mockery context = new Mockery();
        
        final FTPFileUploader delegate = context.mock(FTPFileUploader.class);
        
        String filename = "test";
        byte[] fileData = bytesFromResource("org/atlasapi/feeds/radioplayer/invalidPIFeedTest.xml");
        
        ValidatingFTPFileUploader uploadTask = new ValidatingFTPFileUploader(validator, delegate);
        
        context.checking(new Expectations(){{
            never(delegate).upload(with(any(FTPUpload.class)));
        }});
        
        FTPUploadResult result = uploadTask.upload(new FTPUpload(filename, fileData));
        assertThat(result.type(), is(equalTo(FTPUploadResultType.FAILURE)));
    }

}
