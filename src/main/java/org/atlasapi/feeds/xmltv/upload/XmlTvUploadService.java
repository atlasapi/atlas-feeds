package org.atlasapi.feeds.xmltv.upload;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.FAILURE;

import java.net.ConnectException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.FileUploader;
import org.joda.time.DateTime;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.SystemClock;

public class XmlTvUploadService extends AbstractService implements AsyncFileUploadService {

    private final FileUploader uploader;
    private final String serviceName;
    private final ListeningExecutorService executor;
    private final Clock clock;

    public XmlTvUploadService(String serviceName, FileUploader uploader, ExecutorService executor, Clock clock) {
        this.clock = checkNotNull(clock);
        this.serviceName = checkNotNull(serviceName);
        this.uploader = checkNotNull(uploader);
        this.executor = MoreExecutors.listeningDecorator(checkNotNull(executor));
    }
    
    public XmlTvUploadService(String serviceName, FileUploader uploader) {
        this(serviceName, uploader, Executors.newFixedThreadPool(10,new ThreadFactoryBuilder().setNameFormat(serviceName+"-upload-%s").build()), new SystemClock());
    }
    
    @Override
    protected void doStart() {
        notifyStarted();
    }

    @Override
    protected void doStop() {
        notifyStopped();
    }

    @Override
    public ListenableFuture<FileUploadResult> upload(final FileUpload upload) {
        return executor.submit(new Callable<FileUploadResult>() {

            @Override
            public FileUploadResult call() throws Exception {
                try {
                    uploader.upload(upload);
                    return new FileUploadResult(serviceName(), upload.getFilename(), new DateTime(DateTimeZones.UTC), FileUploadResultType.SUCCESS);
                } catch (ConnectException e) {
                    return failedUploadResult(upload, e).withConnectionSuccess(false);
                } catch (Exception e) {
                    return failedUploadResult(upload, e);
                }
            }
        });
    }
    
    private FileUploadResult failedUploadResult(FileUpload upload, Exception e) {
        return new FileUploadResult(serviceName(), upload.getFilename(), clock.now(), FAILURE).withCause(e).withMessage(e.getMessage());
    }

    @Override
    public String serviceName() {
        return serviceName;
    }
}
