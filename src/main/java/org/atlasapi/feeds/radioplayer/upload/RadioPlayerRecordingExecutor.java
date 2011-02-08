package org.atlasapi.feeds.radioplayer.upload;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RadioPlayerRecordingExecutor {

    private static final int MAX_CONNECTIONS = 5;

    private final RadioPlayerFTPUploadResultRecorder recorder;

    // static to avoid multiple pools
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_CONNECTIONS, new ThreadFactoryBuilder().setNameFormat("RadioPlayerUploader: %s").build());

    public RadioPlayerRecordingExecutor(RadioPlayerFTPUploadResultRecorder recorder) {
        this.recorder = recorder;
    }

    public <T extends RadioPlayerFTPUploadResult> ExecutorCompletionService<T> submit(final Iterable<Callable<T>> callables) {
        ExecutorCompletionService<T> completionService = new ExecutorCompletionService<T>(executor);

        for (final Callable<T> callable : callables) {
            completionService.submit(new Callable<T>() {

                @Override
                public T call() throws Exception {
                    T result = callable.call();
                    recorder.record(result);
                    return result;
                }
            });
        }
        return completionService;
    }

}
