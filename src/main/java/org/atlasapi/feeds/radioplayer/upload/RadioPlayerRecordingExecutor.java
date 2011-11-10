package org.atlasapi.feeds.radioplayer.upload;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

//TODO: Make this extend AbstractExecutorService or similar?
public class RadioPlayerRecordingExecutor {

    private static final int MAX_CONNECTIONS = 5;
    private static final long MAX_RUN_TIME = 2*60*1000;//2 mins

    private final RadioPlayerUploadResultStore recorder;
    private final ExecutorService executor;
    private final TaskCanceller canceller;


    public RadioPlayerRecordingExecutor(RadioPlayerUploadResultStore recorder) {
        this(recorder, Executors.newFixedThreadPool(MAX_CONNECTIONS, new ThreadFactoryBuilder().setNameFormat("RadioPlayerUploader: %s").build()));
    }
    
    public RadioPlayerRecordingExecutor(RadioPlayerUploadResultStore recorder, ExecutorService service) {
        this(recorder, service, new TaskCanceller(MAX_RUN_TIME, TimeUnit.MILLISECONDS));
    }
    
    public RadioPlayerRecordingExecutor(RadioPlayerUploadResultStore recorder, ExecutorService service, TaskCanceller canceller) {
        this.recorder = recorder;
        this.executor = service;
        this.canceller = canceller;
    }

    public <T extends RadioPlayerUploadResult> LinkedBlockingQueue<Future<Iterable<T>>> submit(final Iterable<Callable<Iterable<T>>> callables) {
        final LinkedBlockingQueue<Future<Iterable<T>>> results = new LinkedBlockingQueue<Future<Iterable<T>>>();

        for (final Callable<Iterable<T>> callable : callables) {
            final FutureTask<Iterable<T>> task = canceller.scheduleCancellation(new RecordingCallable<T>(recorder, callable));
            executor.execute(new FutureTask<Iterable<T>>(task, null) {
                @Override
                protected void done() {
                    results.add(task);
                }
            });
        }
        
        return results;
    }
    
    private static class RecordingCallable<T extends RadioPlayerUploadResult> implements Callable<Iterable<T>> {
        private final Callable<Iterable<T>> callable;
        private final RadioPlayerUploadResultStore recorder;

        public RecordingCallable(RadioPlayerUploadResultStore recorder, Callable<Iterable<T>> callable) {
            this.recorder = recorder;
            this.callable = callable;
        }

        @Override
        public Iterable<T> call() throws Exception {
            Iterable<T> results = callable.call();
            for (T result : results) {
                recorder.record(result);
            }
            return results;
        }
    }
    
    public static class TaskCanceller {
        
        private final long delay;
        private final TimeUnit unit;
        private final ScheduledExecutorService canceller;
        
        public TaskCanceller(long delay, TimeUnit unit) {
            this(delay, unit, Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("RadioPlayerUploaderCanceller: %s").build()));
        }

        public TaskCanceller(long delay, TimeUnit unit, ScheduledExecutorService executor) {
            this.delay = delay;
            this.unit = unit;
            canceller = executor;
        }
        
        private ScheduledFuture<Void> submitCancellationCallable(final CancellingTask<?> cancellingTask) {
            return canceller.schedule(new Callable<Void>(){
                public Void call(){
                    cancellingTask.cancel(true);
                    return null;
                }
            }, delay, unit);
        }
        
        public <T> FutureTask<T> scheduleCancellation(Callable<T> callable) {
            return new CancellingTask<T>(callable);
        }
        
        private class CancellingTask<T> extends FutureTask<T> {

            public CancellingTask(Callable<T> callable) {
                super(callable);
            }
            
            @Override
            public void run() {
                Future<Void> cancellationFuture = submitCancellationCallable(this);
                super.run();
                cancellationFuture.cancel(true);
            }
            
        }
    }
    
}
