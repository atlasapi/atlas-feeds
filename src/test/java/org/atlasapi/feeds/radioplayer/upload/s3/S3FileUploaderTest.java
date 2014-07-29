package org.atlasapi.feeds.radioplayer.upload.s3;

import static org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploader.ERROR_KEY;
import static org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploader.HASHCODE_KEY;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.atlasapi.feeds.radioplayer.upload.queue.FileUploader;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploader;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class S3FileUploaderTest {
    
    private static final String FOLDER = "folder";
    private static final String BUCKET = "bucket";
    
    private Clock clock = new TimeMachine(DateTime.now());
    private S3Service service = Mockito.mock(S3Service.class);
    
    private final FileUploader uploader = new S3FileUploader(service, BUCKET, FOLDER, clock);

    @Test
    public void testSuccessfulUpload() throws Exception {
        FileUpload file = createFile();
        S3Object object = createS3Object(file);
        
        S3Bucket s3Bucket = new S3Bucket(BUCKET);
        Mockito.when(service.getBucket(BUCKET)).thenReturn(s3Bucket);
        Mockito.when(service.putObject(s3Bucket, object)).thenReturn(object);
        
        UploadAttempt result = uploader.upload(file);
        
        Mockito.verify(service).getBucket(BUCKET);
//        // TODO not sure how to handle this when equality isn't defined for the class
//        // i hate mockito sometimes
//        Mockito.verify(service).putObject(s3Bucket, Mockito.any(S3Object.class));
        
        assertEquals(FileUploadResultType.SUCCESS, result.uploadResult());
        assertEquals(clock.now(), result.uploadTime());
        assertEquals(object.getMd5HashAsHex(), result.uploadDetails().get(HASHCODE_KEY));
    }

    @Test
    public void testExceptionOnUpload() throws Exception {
        FileUpload file = createFile();
        S3ServiceException exception = new S3ServiceException("invalid file");
        Mockito.when(service.getBucket(BUCKET)).thenThrow(exception);
        
        UploadAttempt result = uploader.upload(file);
        
        assertEquals(FileUploadResultType.FAILURE, result.uploadResult());
        assertEquals(clock.now(), result.uploadTime());
        assertEquals(String.valueOf(exception), result.uploadDetails().get(ERROR_KEY));
    }

    private S3Object createS3Object(FileUpload file) throws NoSuchAlgorithmException, IOException {
        S3Object object = new S3Object(FOLDER + "/" + file.getFilename(), file.getFileData());
        object.setContentType(file.getContentType().toString());
        return object;
    }

    private FileUpload createFile() {
        return FileUpload.fileUpload("20140707_300_PI.xml", new byte[0])
                .withContentType(MimeType.TEXT_XML)
                .build();
    }
}
