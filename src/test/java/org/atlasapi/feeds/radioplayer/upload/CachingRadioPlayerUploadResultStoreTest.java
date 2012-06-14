package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;
import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.DateTimeZones;

public class CachingRadioPlayerUploadResultStoreTest {

    private final Mockery context = new Mockery();
    private final RadioPlayerUploadResultStore delegateStore = context.mock(RadioPlayerUploadResultStore.class);
    
    private final LocalDate day = new LocalDate(DateTimeZones.UTC);
    private final RadioPlayerService service = RadioPlayerServices.all.get("300");
    private final String remoteService = "remoteOne";

    @Test
    public void testBasicRecordAndRead() {

        context.checking(new Expectations(){{
            allowing(delegateStore).record(with(any(RadioPlayerUploadResult.class)));
        }});
        
        CachingRadioPlayerUploadResultStore store = new CachingRadioPlayerUploadResultStore(ImmutableSet.of(remoteService), delegateStore);
        
        store.record(new RadioPlayerUploadResult(PI, service, day, FileUploadResult.successfulUpload(remoteService, "test1")));
        
        DateTime later = new DateTime(DateTimeZones.UTC).plus(5000);
        store.record(new RadioPlayerUploadResult(PI, service, day, new FileUploadResult(remoteService, "test1", later, SUCCESS)));
        
        Set<FileUploadResult> results = ImmutableSet.copyOf(store.resultsFor(PI, remoteService, service, day));
        
        assertThat(results.size(), is(equalTo(1)));
        assertThat(Iterables.getOnlyElement(results).uploadTime(), is(equalTo(later)));
    }

    @Test
    public void testEmptyRead() {
        
        context.checking(new Expectations(){{
            one(delegateStore).resultsFor(with(PI), with(remoteService), with(any(RadioPlayerService.class)), with(any(LocalDate.class)));
            will(returnValue(ImmutableSet.of(FileUploadResult.successfulUpload(remoteService,"test1"))));
        }});
        
        CachingRadioPlayerUploadResultStore store = new CachingRadioPlayerUploadResultStore(ImmutableSet.of(remoteService), delegateStore);
        
        Set<FileUploadResult> results = ImmutableSet.copyOf(store.resultsFor(PI, remoteService, service, day));
        Set<FileUploadResult> results2 = ImmutableSet.copyOf(store.resultsFor(PI, remoteService, service, day));
        
        assertThat(results, is(equalTo(results2)));
        assertThat(results.isEmpty(), is(false));
    }
}