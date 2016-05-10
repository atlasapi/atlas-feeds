package org.atlasapi.feeds.radioplayer.upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedCompiler;
import org.atlasapi.feeds.radioplayer.RadioPlayerFeedSpec;
import org.atlasapi.feeds.radioplayer.outputting.NoItemsException;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;

import com.metabroadcast.common.media.MimeType;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;
import static org.atlasapi.feeds.upload.FileUploadResult.failedUpload;
import static org.atlasapi.feeds.upload.FileUploadResult.successfulUpload;

public abstract class RadioPlayerUploadTask implements Callable<Iterable<RadioPlayerUploadResult>> {

    private static final Logger log = LoggerFactory.getLogger(RadioPlayerUploadTask.class);

    private final Iterable<FileUploadService> remoteTargets;
    protected final AdapterLog adapterLog;
    protected final RadioPlayerFeedSpec spec;
    private final FileType type;
    private final Publisher publisher;

    public RadioPlayerUploadTask(
            FileType type,
            Iterable<FileUploadService> remoteTargets,
            RadioPlayerFeedSpec spec,
            AdapterLog adapterLog,
            Publisher publisher
    ) {
        this.type = type;
        this.remoteTargets = remoteTargets;
        this.spec = spec;
        this.adapterLog = adapterLog;
        this.publisher = publisher;
    }

    @Override
    public Iterable<RadioPlayerUploadResult> call() throws Exception {
        
        logInfo("Starting upload task for %s", spec);
        
        try {
            byte[] fileBytes = getFileContent();

            FileUpload upload = new FileUpload.Builder(spec.filename(), fileBytes)
                    .withContentType(MimeType.TEXT_XML)
                    .build();

            logInfo("Compiled file for uploading for %s", spec);

            Iterable<RadioPlayerUploadResult> results = doUploads(upload);

            logResults(results);

            return results;
        } catch (Exception e) {
            logError(e, "Failed upload task for %s", spec);
            return failedUploads(e);
        }
    }

    protected abstract boolean isFailure(NoItemsException e);

    private byte[] getFileContent() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RadioPlayerFeedCompiler.valueOf(publisher, type).compileFeedFor(spec, out);
        return out.toByteArray();
    }

    private Iterable<RadioPlayerUploadResult> doUploads(final FileUpload upload) {
        ImmutableList.Builder<RadioPlayerUploadResult> results = ImmutableList.builder();
        for (FileUploadService target : remoteTargets) {
            try {
                results.add(uploadTo(target, upload));
            } catch (NoItemsException e) {
                if (isFailure(e)) {
                    throw e;
                }
                results.add(nothingToUpload(target));
            }
            catch (InterruptedException e) {
                logError(e, "Upload of %s was interrupted", spec);
                results.add(failure(target, e));
                break;
            }
        }
        return results.build();
    }

    private Iterable<RadioPlayerUploadResult> failedUploads(final Exception e) {
        return Iterables.transform(
                remoteTargets,
                new Function<FileUploadService, RadioPlayerUploadResult>() {
                        @Override
                    public RadioPlayerUploadResult apply(FileUploadService input) {
                        return failure(input, e);
                    }
                }
        );
    }

    private RadioPlayerUploadResult uploadTo(FileUploadService uploadService, FileUpload upload)
            throws InterruptedException {
        return new RadioPlayerUploadResult(
                type,
                spec.getService(),
                spec.getDay(),
                uploadService.upload(upload)
        );
    }

    private RadioPlayerUploadResult failure(FileUploadService input, Exception e) {
        return new RadioPlayerUploadResult(
                type,
                spec.getService(),
                spec.getDay(),
                failedUpload(
                        input.serviceIdentifier(),
                        spec.filename()).withCause(e).copyWithMessage(e.getMessage()
                )
        );
    }

    private RadioPlayerUploadResult nothingToUpload(FileUploadService input) {
        return new RadioPlayerUploadResult(
                type,
                spec.getService(),
                spec.getDay(),
                successfulUpload(input.serviceIdentifier(), spec.filename())
        );
    }

    private void logResults(Iterable<RadioPlayerUploadResult> results) {
        boolean allSucceeded = true;

        for (RadioPlayerUploadResult result : results) {
            if (!SUCCESS.equals(result.getUpload().type())) {
                allSucceeded = false;

                Exception exception = result.getUpload().exception();
                if (exception != null) {
                    logError(exception, "Failed upload task for %s, service: %s, message: %s",
                            spec, result.getService(), result.getUpload().message());
                } else {
                    logError("Failed upload task for %s, service: %s, message: %s",
                            spec, result.getService(), result.getUpload().message());
                }
            }
        }

        if (allSucceeded) {
            logInfo("Successfully completed upload task for %s", spec);
        }
    }

    private void logInfo(String message, Object... args) {
        adapterLog.record(
                AdapterLogEntry.infoEntry()
                        .withDescription(message, args)
                        .withSource(getClass())
        );
        log.info(String.format(message, args));
    }

    private void logError(Exception e, String message, Object... args) {
        adapterLog.record(
                AdapterLogEntry.errorEntry()
                        .withCause(e)
                        .withDescription(message, args)
                        .withSource(getClass())
        );
        log.error(String.format(message, args), e);
    }

    private void logError(String message, Object... args) {
        adapterLog.record(
                AdapterLogEntry.errorEntry()
                        .withDescription(message, args)
                        .withSource(getClass())
        );
        log.error(String.format(message, args));
    }
}
