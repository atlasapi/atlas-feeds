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
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

@RunWith(JMock.class)
public class ValidatingFTPFileUploadTest {

    private final Mockery context = new Mockery();
    private final FileUploader delegate = context.mock(FileUploader.class);
    private final String filename = "test";
    private final XMLValidator validator = buildValidator();
    private final ValidatingFileUploader uploadTask = new ValidatingFileUploader(validator, delegate);

    @Test
    public void testCallsDelegateOnSuccess() throws Exception {
        
        byte[] fileData = bytesFromResource("org/atlasapi/feeds/radioplayer/basicPIFeedTest.xml");
        
        context.checking(new Expectations(){{
            one(delegate).upload(with(any(FileUpload.class)));
        }});
        
        uploadTask.upload(new FileUpload(filename , fileData));
        
    }

    @Test(expected=ValidityException.class)
    public void testDoesntCallDelegateOnFailure() throws Exception {
        
        byte[] fileData = bytesFromResource("org/atlasapi/feeds/radioplayer/invalidPIFeedTest.xml");
        
        context.checking(new Expectations(){{
            never(delegate).upload(with(any(FileUpload.class)));
        }});
        
        uploadTask.upload(new FileUpload(filename, fileData));
    }

    private XMLValidator buildValidator() {
        try {
            return XMLValidator.forSchemas(ImmutableSet.<InputStream>of(
                    Resources.getResource("org/atlasapi/feeds/radioplayer/xml.xsd").openStream(),
                    Resources.getResource("org/atlasapi/feeds/radioplayer/epgSI_10.xsd").openStream(),
                    Resources.getResource("org/atlasapi/feeds/radioplayer/epgSchedule_10.xsd").openStream()
            ));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    private byte[] bytesFromResource(String file) throws IOException {
        return Resources.toByteArray(Resources.getResource(file));
    }
}
