package org.atlasapi.feeds.radioplayer.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerRecordingExecutor.TaskCanceller;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerRecordingExecutorTest {

    private final Mockery context = new Mockery();
    private final RadioPlayerFTPUploadResultStore recorder = context.mock(RadioPlayerFTPUploadResultStore.class);
    private final RadioPlayerService service = RadioPlayerServices.all.get("340");
    private final LocalDate date = new LocalDate(DateTimeZones.UTC);

    @Test
    public void testStandardSubmit() {

        ExecutorService executorService = MoreExecutors.sameThreadExecutor();
        RadioPlayerRecordingExecutor executor = new RadioPlayerRecordingExecutor(recorder, executorService);

        context.checking(new Expectations() {{
            one(recorder).record(with(any(RadioPlayerFTPUploadResult.class)));
        }});

        executor.submit(ImmutableSet.<Callable<RadioPlayerFTPUploadResult>> of(new Callable<RadioPlayerFTPUploadResult>() {

            @Override
            public RadioPlayerFTPUploadResult call() throws Exception {
                return new RadioPlayerFTPUploadResult(new FTPUploadResult("file", new DateTime(), FTPUploadResultType.SUCCESS), service, date);
            }
        }));

        context.assertIsSatisfied();
    }

    @Test(expected=CancellationException.class)
    public void testTaskGetsCancelled() throws InterruptedException, ExecutionException {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        RadioPlayerRecordingExecutor executor = new RadioPlayerRecordingExecutor(recorder, executorService, new TaskCanceller(10, TimeUnit.MILLISECONDS));

        context.checking(new Expectations() {{
            never(recorder).record(with(any(RadioPlayerFTPUploadResult.class)));
        }});

        LinkedBlockingQueue<Future<RadioPlayerFTPUploadResult>> results = executor.submit(ImmutableSet.<Callable<RadioPlayerFTPUploadResult>> of(new Callable<RadioPlayerFTPUploadResult>() {
            @Override
            public RadioPlayerFTPUploadResult call() throws Exception {
                Thread.sleep(20000);
                return null;
            }
        }));

        Future<RadioPlayerFTPUploadResult> result = results.take();
        
        assertThat(result.isDone(), is(true));
        
        context.assertIsSatisfied();
        
        result.get();
    }
}
