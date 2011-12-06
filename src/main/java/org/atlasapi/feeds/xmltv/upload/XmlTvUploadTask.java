package org.atlasapi.feeds.xmltv.upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;
import org.atlasapi.feeds.xmltv.XmlTvChannelLookup;
import org.atlasapi.feeds.xmltv.XmlTvChannelsCompiler;
import org.atlasapi.feeds.xmltv.XmlTvFeedCompiler;
import org.atlasapi.media.entity.Channel;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ranges;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.DateTimeZones;

public class XmlTvUploadTask extends ScheduledTask {

    private final AsyncFileUploadService uploadService;
    private final XmlTvChannelLookup channelLookup;
    private final XmlTvFeedCompiler feedCompiler;
    private final FileUploadResultStore resultStore;
    private final XmlTvChannelsCompiler channelCompiler;
    private final AdapterLog log;

    public XmlTvUploadTask(AsyncFileUploadService uploadService, FileUploadResultStore resultStore, XmlTvFeedCompiler feedCompiler, XmlTvChannelLookup channelLookup, AdapterLog log) {
        this.uploadService = uploadService;
        this.resultStore = resultStore;
        this.feedCompiler = feedCompiler;
        this.channelLookup = channelLookup;
        this.log = log;
        this.channelCompiler = new XmlTvChannelsCompiler(channelLookup);
    }
    
    @Override
    protected void runTask() {
        LocalDate startDay = new LocalDate(DateTimeZones.LONDON);

        ListenableFuture<List<FileUploadResult>> uploadResults = Futures.allAsList(
            ImmutableList.<ListenableFuture<FileUploadResult>>builder()
                .add(uploadChannelsDat("channels.dat"))
                .addAll(uploadChannels(startDay))
            .build()
        );
        
        
        List<FileUploadResult> results;
        try {
            results = uploadResults.get();
            for (FileUploadResult uploadResult : results) {
                resultStore.store(uploadResult.filename(), uploadResult);
            }
        } catch (Exception e) {
            log.record(AdapterLogEntry.errorEntry().withCause(e).withSource(getClass()).withDescription("Exception upload XMLTV feeds"));
            throw Throwables.propagate(e);
        }
       
    }

    private Iterable<ListenableFuture<FileUploadResult>> uploadChannels(final LocalDate startDay) {
        return Iterables.transform(channelLookup.entrySet(), new Function<Entry<Integer,Channel>, ListenableFuture<FileUploadResult>>(){

            @Override
            public ListenableFuture<FileUploadResult> apply(Entry<Integer, Channel> channel) {
                return upload(String.format("%s.dat", channel.getKey()), startDay, channel);
            }});
    }

    private ListenableFuture<FileUploadResult> uploadChannelsDat(String filename) {
        ByteArrayOutputStream writeTo = new ByteArrayOutputStream();
        try {
            channelCompiler.compileChannelsFeed(writeTo);
        } catch (IOException e) {
            return Futures.immediateFuture(failedUpload(filename, e));
        }
        return uploadService.upload(new FileUpload(filename, writeTo.toByteArray()));
    }

    private ListenableFuture<FileUploadResult> upload(String filename, LocalDate startDay, Entry<Integer, Channel> channel) {
        ByteArrayOutputStream writeTo = new ByteArrayOutputStream();
        try {
            feedCompiler.compileChannelFeed(Ranges.closed(startDay, startDay.plusWeeks(2)), channel.getValue(), writeTo);
        } catch (IOException e) {
            return Futures.immediateFuture(failedUpload(filename, e));
        }
        return uploadService.upload(new FileUpload(filename, writeTo.toByteArray()));
    }

    private FileUploadResult failedUpload(String filename, Exception e) {
        return FileUploadResult.failedUpload(uploadService.serviceName(), filename).withCause(e).withMessage(e.getMessage());
    }

}
