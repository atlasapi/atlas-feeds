package org.atlasapi.feeds.radioplayer.upload.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploaderProvider;
import org.atlasapi.feeds.xml.XMLValidator;
import org.jets3t.service.S3Service;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class UploadServicesSupplierTest {
    
    private XMLValidator validator = Mockito.mock(XMLValidator.class);
    private Clock clock = new TimeMachine(DateTime.now(DateTimeZone.UTC));
    private FileUploaderProvider uploaderProvider = new S3FileUploaderProvider(Mockito.mock(S3Service.class), "bucket", clock);
    private final UploadServicesSupplier supplier = new UploadServicesSupplier(ImmutableList.of(uploaderProvider), validator);

    @Test
    public void testSuppliesUploadServiceProvider() {
        
        Optional<FileUploader> uploader = supplier.get(UploadService.S3, clock.now(), FileType.PI);
        ValidatingFileUploader s3Uploader = (ValidatingFileUploader) uploader.get();
        
        assertEquals(uploaderProvider.get(clock.now(), FileType.PI), s3Uploader.delegate());
    }

    @Test
    public void testAbsentForNonPresentUploadService() {
        assertFalse(supplier.get(UploadService.HTTPS, new DateTime(), FileType.PI).isPresent());
    }
}
