package org.atlasapi.feeds.radioplayer.upload.queue;

import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class UploadAttemptOrderingTest {

    private Clock clock = new TimeMachine(DateTime.now(DateTimeZone.UTC));
    
    @Test
    public void testOrdersByUploadTime() {
        ImmutableMap<String, String> details = ImmutableMap.<String, String>of();
        
        UploadAttempt earlier = UploadAttempt.successfulUpload(clock.now().minusHours(1), details);
        UploadAttempt later = UploadAttempt.successfulUpload(clock.now(), details);
        
        ImmutableSet<UploadAttempt> attempts = ImmutableSet.of(earlier, later);
        
        assertEquals(earlier, Ordering.natural().min(attempts));
        assertEquals(later, Ordering.natural().max(attempts));
    }

    @Test
    public void testNullUploadTimeComesLast() {
        UploadAttempt nullUploadTime = UploadAttempt.enqueuedAttempt();
        UploadAttempt timed = UploadAttempt.successfulUpload(clock.now(), ImmutableMap.<String, String>of());
        
        ImmutableSet<UploadAttempt> attempts = ImmutableSet.of(nullUploadTime, timed);
        
        assertEquals(nullUploadTime, Ordering.natural().min(attempts));
        assertEquals(timed, Ordering.natural().max(attempts));
    }
}
