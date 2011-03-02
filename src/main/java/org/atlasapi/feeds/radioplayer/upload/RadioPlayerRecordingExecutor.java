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

public class RadioPlayerRecordingExecutor {

    private static final int MAX_CONNECTIONS = 5;
    private static final long MAX_RUN_TIME = 2*60*1000;//2 mins

    private final RadioPlayerFTPUploadResultStore recorder;
    private final ExecutorService executor;
    private final TaskCanceller canceller;


    public RadioPlayerRecordingExecutor(RadioPlayerFTPUploadResultStore recorder) {
        this(recorder, Executors.newFixedThreadPool(MAX_CONNECTIONS, new ThreadFactoryBuilder().setNameFormat("RadioPlayerUploader: %s").build()));
    }
    
    public RadioPlayerRecordingExecutor(RadioPlayerFTPUploadResultStore recorder, ExecutorService service) {
        this(recorder, service, new TaskCanceller(MAX_RUN_TIME, TimeUnit.MILLISECONDS));
    }
    
    public RadioPlayerRecordingExecutor(RadioPlayerFTPUploadResultStore recorder, ExecutorService service, TaskCanceller canceller) {
        this.recorder = recorder;
        this.executor = service;
        this.canceller = canceller;
    }

    public <T extends RadioPlayerFTPUploadResult> LinkedBlockingQueue<Future<T>> submit(final Iterable<Callable<T>> callables) {
        final LinkedBlockingQueue<Future<T>> results = new LinkedBlockingQueue<Future<T>>();

        for (final Callable<T> callable : callables) {
            final FutureTask<T> task = canceller.scheduleCancellation(new RecordingCallable<T>(recorder, callable));
            executor.execute(new FutureTask<T>(task, null) {
                @Override
                protected void done() {
                    results.add(task);
                }
            });
        }
        
        return results;
    }
    
    private static class RecordingCallable<T extends RadioPlayerFTPUploadResult> implements Callable<T> {
        private final Callable<? extends T> callable;
        private final RadioPlayerFTPUploadResultStore recorder;

        public RecordingCallable(RadioPlayerFTPUploadResultStore recorder, Callable<? extends T> callable) {
            this.recorder = recorder;
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            T result = callable.call();
            recorder.record(result);
            return result;
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
