package org.atlasapi.reporting.telescope;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.metabroadcast.columbus.telescope.client.TelescopeReporterFactory;
import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;
import com.metabroadcast.common.properties.Configurer;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedsTelescopeReporterFactory extends TelescopeReporterFactory {
    private static final Logger log = LoggerFactory.getLogger(FeedsTelescopeReporterFactory.class);

    private FeedsTelescopeReporterFactory(
            @Nonnull String env,
            @Nonnull String host,
            @Nonnull ThreadPoolExecutor executor,
            @Nullable MetricRegistry metricsRegistry,
            @Nullable String metricsPrefix) {
        super(env, host, executor, metricsRegistry, metricsPrefix);
    }

    //Surprisingly, this will draw the actual configuration from atlas config.

    //If fewer than this threads are running, a new thread is created. Else things are queued.
    private static final int CORE_THREADS = Integer.parseInt(Configurer.get("telescope.coreReportingThreads").get());
    //If the queue is full, spawn a new thread up to this number.
    private static final int MAX_THREADS = Integer.parseInt(Configurer.get("telescope.maxReportingThreads").get());
    //If new threads cant be spawned. Things that don't fit go to the RejectedExecutionHandler
    private static final int QUEUE_SIZE = Integer.parseInt(Configurer.get("telescope.queueSize").get());
    private static final String THREAD_NAME = Configurer.get("telescope.reportingThreadName").get();
    private static final String METRICS_PREFIX = Configurer.get("telescope.metricsPrefix").get();


    //Implement this as a Singleton
    private static FeedsTelescopeReporterFactory INSTANCE;
    public static synchronized FeedsTelescopeReporterFactory getInstance() {
        if (INSTANCE == null) {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    CORE_THREADS,
                    MAX_THREADS,
                    10, TimeUnit.SECONDS, //time to put idle threads to sleep.
                    new ArrayBlockingQueue<>(QUEUE_SIZE),
                    new ThreadFactoryBuilder().setNameFormat(THREAD_NAME + " Thread %d").build(),
                    new TelescopeReporterFactory.RejectedExecutionHandlerImpl()
            );
            executor.allowCoreThreadTimeOut(true);

            INSTANCE = new FeedsTelescopeReporterFactory( //this should always produce an instance.
                    Configurer.get("telescope.environment").get(),
                    Configurer.get("telescope.host").get(),
                    executor,
                    null, null //metricsRegistry and METRICS_PREFIX, null until further notice.
            );
        }

        return INSTANCE;
    }

    public FeedsTelescopeReporter getTelescopeReporter(TelescopeReporterName reporterName){
        return new FeedsTelescopeReporter(reporterName, this.getEnvironment(), this.getClient());
    }
}
