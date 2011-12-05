package org.atlasapi.feeds.xmltv.upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map.Entry;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;
import org.atlasapi.feeds.xmltv.XmlTvChannelLookup;
import org.atlasapi.feeds.xmltv.XmlTvChannelsCompiler;
import org.atlasapi.feeds.xmltv.XmlTvFeedCompiler;
import org.atlasapi.media.entity.Channel;
import org.joda.time.LocalDate;

import com.google.common.collect.Ranges;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.DateTimeZones;

public class XmlTvUploadTask extends ScheduledTask {

    private final AsyncFileUploadService uploadService;
    private final XmlTvChannelLookup channelLookup;
    private final XmlTvFeedCompiler feedCompiler;
    private final FileUploadResultStore resultStore;
    private final XmlTvChannelsCompiler channelCompiler;

    public XmlTvUploadTask(AsyncFileUploadService uploadService, FileUploadResultStore resultStore, XmlTvFeedCompiler feedCompiler, XmlTvChannelLookup channelLookup) {
        this.uploadService = uploadService;
        this.resultStore = resultStore;
        this.feedCompiler = feedCompiler;
        this.channelLookup = channelLookup;
        this.channelCompiler = new XmlTvChannelsCompiler(channelLookup);
    }
    
    @Override
    protected void runTask() {
        LocalDate startDay = new LocalDate(DateTimeZones.LONDON);

        String filename = "channels.dat";
        Futures.addCallback(uploadChannels(filename),storingCallback(filename, filename));
        
        for (final Entry<Integer, Channel> channel : channelLookup.entrySet()) {
            filename = String.format("%s.dat", channel.getKey());
            String storeKey = String.valueOf(channel.getKey());
            Futures.addCallback(upload(filename, startDay, channel), storingCallback(filename, storeKey));
        }
    }

    private FutureCallback<FileUploadResult> storingCallback(final String filename, final String storeKey) {
        return new FutureCallback<FileUploadResult>() {

            @Override
            public void onSuccess(FileUploadResult result) {
                write(result);
            }

            @Override
            public void onFailure(Throwable t) {
                write(failedUpload(filename, new RuntimeException(t)));
            }

            private void write(FileUploadResult upload) {
                resultStore.store(storeKey, upload);
            }
        };
    }

    private ListenableFuture<FileUploadResult> uploadChannels(String filename) {
        ByteArrayOutputStream writeTo = new ByteArrayOutputStream();
        try {
            channelCompiler.compileChannelsFeed(writeTo);
        } catch (IOException e) {
            final SettableFuture<FileUploadResult> result = SettableFuture.create();
            result.set(failedUpload(filename, e));
            return result;
        }
        return uploadService.upload(new FileUpload(filename, writeTo.toByteArray()));
    }

    private ListenableFuture<FileUploadResult> upload(String filename, LocalDate startDay, Entry<Integer, Channel> channel) {
        ByteArrayOutputStream writeTo = new ByteArrayOutputStream();
        try {
            feedCompiler.compileChannelFeed(Ranges.closed(startDay, startDay.plusWeeks(2)), channel.getValue(), writeTo);
        } catch (IOException e) {
            final SettableFuture<FileUploadResult> result = SettableFuture.create();
            result.set(failedUpload(filename, e));
            return result;
        }
        return uploadService.upload(new FileUpload(filename, writeTo.toByteArray()));
    }

    private FileUploadResult failedUpload(String filename, Exception e) {
        return FileUploadResult.failedUpload(uploadService.serviceName(), filename).withCause(e).withMessage(e.getMessage());
    }

}
