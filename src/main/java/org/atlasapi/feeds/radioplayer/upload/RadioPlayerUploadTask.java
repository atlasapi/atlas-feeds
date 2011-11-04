package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.failedUpload;
import static org.atlasapi.persistence.logging.AdapterLogEntry.debugEntry;
import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedCompiler;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.outputting.NoItemsException;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerUploadTask implements Callable<Iterable<RadioPlayerUploadResult>> {

    private final Iterable<FileUploadService> remoteTargets;
    private final LocalDate day;
    private final RadioPlayerService service;
    private final AdapterLog log;

    public RadioPlayerUploadTask(Iterable<FileUploadService> remoteTargets, LocalDate day, RadioPlayerService service, AdapterLog log) {
        this.remoteTargets = remoteTargets;
        this.day = day;
        this.service = service;
        this.log = log;
    }

    @Override
    public Iterable<RadioPlayerUploadResult> call() throws Exception {
        final String filename = String.format("%s_%s_PI.xml", day.toString("yyyyMMdd"), service.getRadioplayerId());
        
        try {
            byte[] filebytes = getFileContent();
            FileUpload upload = new FileUpload(filename, filebytes);
            return doUploads(upload);
        } catch (NoItemsException e) {
            logNotItemsException(filename, e);
            return failedUploads(filename, e);
        }
        
    }

    private byte[] getFileContent() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RadioPlayerFeedCompiler.valueOf("PI").compileFeedFor(day, service, out);
        return out.toByteArray();
    }
    
    private Iterable<RadioPlayerUploadResult> doUploads(final FileUpload upload) {
        ImmutableList.Builder<RadioPlayerUploadResult> results = ImmutableList.builder();
        for (FileUploadService target : remoteTargets) {
            try {
                results.add(uploadTo(target, upload));
            } catch (InterruptedException e) {
                log.record(errorEntry().withCause(e).withDescription("Upload of " + upload.getFilename() + " was interrupted").withSource(getClass()));
                results.add(failure(target, upload.getFilename(), e));
                break;
            }
        }
        return results.build();
    }

    private RadioPlayerUploadResult uploadTo(FileUploadService uploadService, FileUpload upload) throws InterruptedException {
        return new RadioPlayerUploadResult(uploadService.serviceIdentifier(), service, day, uploadService.upload(upload));
    }

    private Iterable<RadioPlayerUploadResult> failedUploads(final String filename, final NoItemsException e) {
        return Iterables.transform(remoteTargets, new Function<FileUploadService, RadioPlayerUploadResult>() {
            @Override
            public RadioPlayerUploadResult apply(FileUploadService input) {
                return failure(input, filename, e);
            }

        });
    }
    private RadioPlayerUploadResult failure(FileUploadService input, final String filename, final Exception e) {
        return new RadioPlayerUploadResult(input.serviceIdentifier(), service, day, failedUpload(filename).withCause(e).withMessage(e.getMessage()));
    }

    private void logNotItemsException(String filename, NoItemsException e) {
        if( log != null) {
            if (!day.isAfter(new LocalDate(DateTimeZones.UTC).plusDays(1)) && !RadioPlayerServices.untracked.contains(service)) {
                log.record(errorEntry().withDescription("No items for " + filename).withSource(getClass()).withCause(e));
            } else {
                log.record(debugEntry().withDescription("No items for " + filename).withSource(getClass()).withCause(e));
            }
        }
    }

}
