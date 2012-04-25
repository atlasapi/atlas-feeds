package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.failedUpload;
import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedCompiler;
import org.atlasapi.feeds.radioplayer.RadioPlayerFeedSpec;
import org.atlasapi.feeds.radioplayer.outputting.NoItemsException;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.persistence.logging.AdapterLog;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public abstract class RadioPlayerUploadTask implements Callable<Iterable<RadioPlayerUploadResult>> {
    
    private final Iterable<FileUploadService> remoteTargets;
    protected final AdapterLog log;
    protected final RadioPlayerFeedSpec spec;
    private final FileType type;

    public RadioPlayerUploadTask(FileType type, Iterable<FileUploadService> remoteTargets, RadioPlayerFeedSpec spec, AdapterLog log) {
        this.type = type;
        this.remoteTargets = remoteTargets;
        this.spec = spec;
        this.log = log;
    }

    @Override
    public Iterable<RadioPlayerUploadResult> call() throws Exception {
        final String filename = spec.filename();
        
        try {
            byte[] filebytes = getFileContent();
            FileUpload upload = new FileUpload(filename, filebytes);
            return doUploads(upload);
        } catch (NoItemsException e) {
            logNotItemsException(e);
            return failedUploads(e);
        }
    }

    private byte[] getFileContent() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RadioPlayerFeedCompiler.valueOf(type).compileFeedFor(spec, out);
        return out.toByteArray();
    }
    
    private Iterable<RadioPlayerUploadResult> doUploads(final FileUpload upload) {
        ImmutableList.Builder<RadioPlayerUploadResult> results = ImmutableList.builder();
        for (FileUploadService target : remoteTargets) {
            try {
                results.add(uploadTo(target, upload));
            } catch (InterruptedException e) {
                log.record(errorEntry().withCause(e).withDescription("Upload of " + upload.getFilename() + " was interrupted").withSource(getClass()));
                results.add(failure(target, e));
                break;
            }
        }
        return results.build();
    }

    private Iterable<RadioPlayerUploadResult> failedUploads(final NoItemsException e) {
        return Iterables.transform(remoteTargets, new Function<FileUploadService, RadioPlayerUploadResult>() {
            @Override
            public RadioPlayerUploadResult apply(FileUploadService input) {
                return failure(input, e);
            }

        });
    }
    
    private RadioPlayerUploadResult uploadTo(FileUploadService uploadService, FileUpload upload) throws InterruptedException {
        return new RadioPlayerUploadResult(type, spec.getService(), spec.getDay(), uploadService.upload(upload));
    }
    
    private RadioPlayerUploadResult failure(FileUploadService input, Exception e) {
        return new RadioPlayerUploadResult(type, spec.getService(), spec.getDay(), failedUpload(input.serviceIdentifier(), spec.filename()).withCause(e).withMessage(e.getMessage()));
    }
    
    protected abstract void logNotItemsException(NoItemsException e);
}
