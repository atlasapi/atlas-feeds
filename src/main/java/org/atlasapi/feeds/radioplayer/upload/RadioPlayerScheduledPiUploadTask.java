package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.reporting.telescope.FeedsReporterNames;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

public class RadioPlayerScheduledPiUploadTask extends ScheduledTask {

    private final Iterable<RadioPlayerService> services;
    private final RadioPlayerRecordingExecutor executor;
    private final RadioPlayerUploadServicesSupplier uploadersSupplier;
    private final DayRangeGenerator dayRangeGenerator;
    private final AdapterLog log;
    private final Publisher publisher;
    private ChannelResolver channelResolver;
    private FeedsReporterNames telescopeName;

    public RadioPlayerScheduledPiUploadTask(
            RadioPlayerUploadServicesSupplier uploadServicesSupplier,
            RadioPlayerRecordingExecutor executor, Iterable<RadioPlayerService> services,
            DayRangeGenerator dayRangeGenerator, AdapterLog log, Publisher publisher,
            ChannelResolver channelResolver,
            FeedsReporterNames telescopeName) {
        this.uploadersSupplier = uploadServicesSupplier;
        this.executor = executor;
        this.services = services;
        this.dayRangeGenerator = dayRangeGenerator;
        this.log = log;
        this.publisher = publisher;
        this.channelResolver = channelResolver;
        this.telescopeName = telescopeName;
    }

    @Override
    public void runTask() {
        new RadioPlayerPiBatchUploadTask(
                uploadersSupplier.get(
                        new DateTime(DateTimeZone.UTC),
                        FileType.PI
                ),
                executor,
                services,
                dayRangeGenerator.generate(new LocalDate(DateTimeZones.UTC)),
                log,
                publisher,
                channelResolver,
                telescopeName
        ).run();
        
    }

}
