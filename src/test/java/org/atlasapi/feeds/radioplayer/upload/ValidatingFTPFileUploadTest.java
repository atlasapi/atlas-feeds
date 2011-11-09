package org.atlasapi.feeds.radioplayer.upload;

import java.io.IOException;
import java.io.InputStream;

import nu.xom.ValidityException;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.ValidatingFileUploader;
import org.atlasapi.feeds.xml.XMLValidator;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class ValidatingFTPFileUploadTest {

    @Test
    public void testCallsDelegateOnSuccess() throws Exception {
        
        XMLValidator validator = XMLValidator.forSchemas(ImmutableSet.<InputStream>of(
                Resources.getResource("org/atlasapi/feeds/radioplayer/xml.xsd").openStream(),
                Resources.getResource("org/atlasapi/feeds/radioplayer/epgSI_10.xsd").openStream(),
                Resources.getResource("org/atlasapi/feeds/radioplayer/epgSchedule_10.xsd").openStream()
        ));
        
        Mockery context = new Mockery();
        
        final FileUploader delegate = context.mock(FileUploader.class);
        
        String filename = "test";
        byte[] fileData = bytesFromResource("org/atlasapi/feeds/radioplayer/basicPIFeedTest.xml");
        
        ValidatingFileUploader uploadTask = new ValidatingFileUploader(validator, delegate);
        
        context.checking(new Expectations(){{
            one(delegate).upload(with(any(FileUpload.class)));
        }});
        
        uploadTask.upload(new FileUpload(filename , fileData));
        
    }
    
    private byte[] bytesFromResource(String file) throws IOException {
        return Resources.toByteArray(Resources.getResource(file));
    }

    @Test(expected=ValidityException.class)
    public void testDoesntCallDelegateOnFailure() throws Exception {
        XMLValidator validator = XMLValidator.forSchemas(ImmutableSet.<InputStream>of(
                Resources.getResource("org/atlasapi/feeds/radioplayer/xml.xsd").openStream(),
                Resources.getResource("org/atlasapi/feeds/radioplayer/epgSI_10.xsd").openStream(),
                Resources.getResource("org/atlasapi/feeds/radioplayer/epgSchedule_10.xsd").openStream()
        ));
        
        Mockery context = new Mockery();
        
        final FileUploader delegate = context.mock(FileUploader.class);
        
        String filename = "test";
        byte[] fileData = bytesFromResource("org/atlasapi/feeds/radioplayer/invalidPIFeedTest.xml");
        
        ValidatingFileUploader uploadTask = new ValidatingFileUploader(validator, delegate);
        
        context.checking(new Expectations(){{
            never(delegate).upload(with(any(FileUpload.class)));
        }});
        
        uploadTask.upload(new FileUpload(filename, fileData));
    }

}
