package org.atlasapi.feeds.radioplayer.upload.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.s3.S3RemoteCheckServiceProvider;
import org.jets3t.service.S3Service;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class RemoteCheckerSupplierTest {
    
    private Clock clock = new TimeMachine(DateTime.now(DateTimeZone.UTC));
    private RemoteCheckServiceProvider remoteCheckProvider = new S3RemoteCheckServiceProvider(Mockito.mock(S3Service.class), "bucket");
    private final RemoteCheckerSupplier supplier = new RemoteCheckerSupplier(ImmutableList.of(remoteCheckProvider ));

    @Test
    public void testSuppliesRemoteCheckerProvider() {
        
        Optional<RemoteCheckService> checker = supplier.get(UploadService.S3, clock.now(), FileType.PI);
        RemoteCheckService s3Checker = checker.get();
        assertEquals(remoteCheckProvider.get(clock.now(), FileType.PI), s3Checker);
    }

    @Test
    public void testAbsentForNonPresentRemoteChecker() {
        assertFalse(supplier.get(UploadService.HTTPS, new DateTime(), FileType.PI).isPresent());
    }
}
