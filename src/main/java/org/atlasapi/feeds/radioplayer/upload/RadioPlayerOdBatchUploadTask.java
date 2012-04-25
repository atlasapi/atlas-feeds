package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentCategory;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.listing.ContentListingCriteria;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.metabroadcast.common.base.MoreOrderings;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerOdBatchUploadTask implements Runnable {
    
    private final Ordering<Broadcast> byTransmissionTime = MoreOrderings.<Broadcast, DateTime>transformingOrdering(Broadcast.TO_TRANSMISSION_TIME, Ordering.<DateTime>natural());
    
    private final Iterable<FileUploadService> uploaders;
    private final RadioPlayerRecordingExecutor executor;
    private final Iterable<RadioPlayerService> services;
    private final AdapterLog log;
    private final LastUpdatedContentFinder lastUpdatedContentFinder;
    private final ContentLister contentLister;
    private final boolean fullSnapshot;
    private final LocalDate day;
    private final Optional<DateTime> since;

    public RadioPlayerOdBatchUploadTask(Iterable<FileUploadService> uploaders, RadioPlayerRecordingExecutor executor, Iterable<RadioPlayerService> services, LocalDate day, boolean fullSnapshot, AdapterLog log, LastUpdatedContentFinder lastUpdatedContentFinder, ContentLister contentLister) {
        this.uploaders = uploaders;
        this.executor = executor;
        this.services = services;
        this.day = day;
        this.fullSnapshot = fullSnapshot;
        this.log = log;
        this.lastUpdatedContentFinder = lastUpdatedContentFinder;
        this.contentLister = contentLister;
        this.since = fullSnapshot ? Optional.<DateTime>absent() : Optional.of(day.toDateTimeAtStartOfDay(DateTimeZone.UTC).minusHours(2));
    }
    
    private SetMultimap<RadioPlayerService, String> getServiceToUrisMap() {
        
        HashMultimap<RadioPlayerService, String> serviceToUris = HashMultimap.create();
        
        Iterator<Content> content;
        if (fullSnapshot) {
            content = contentLister.listContent(new ContentListingCriteria.Builder().forPublisher(Publisher.BBC).forContent(ContentCategory.ITEMS).build());
        } else {
            content = lastUpdatedContentFinder.updatedSince(Publisher.BBC, since.get());
        }
        
        while (content.hasNext()) {
            Item item = (Item) content.next();
            
            Set<Broadcast> allBroadcasts = Sets.newHashSet();
            for (Version version : item.getVersions()) {
                allBroadcasts.addAll(version.getBroadcasts());
            }
            
            if (!allBroadcasts.isEmpty()) {
                Broadcast firstBroadcast = byTransmissionTime.min(allBroadcasts);
                
                RadioPlayerService service = RadioPlayerServices.serviceUriToService.get(firstBroadcast.getBroadcastOn());
                if (service != null) {
                    serviceToUris.put(service, item.getCanonicalUri());
                }
            }
        }
        
        return serviceToUris;
    }
    
    @Override
    public void run() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        int serviceCount = Iterables.size(services);
        log.record(AdapterLogEntry.infoEntry().withDescription("Radioplayer OD Uploader starting for %s services", serviceCount));
        
        List<Callable<Iterable<RadioPlayerUploadResult>>> uploadTasks = Lists.newArrayListWithCapacity(serviceCount);
        SetMultimap<RadioPlayerService,String> serviceToUris = getServiceToUrisMap();
        
        for (RadioPlayerService service : services) {
            uploadTasks.add(new RadioPlayerOdUploadTask(uploaders, since, day, service, serviceToUris.get(service), log));
        }

        LinkedBlockingQueue<Future<Iterable<RadioPlayerUploadResult>>> futureResults = executor.submit(uploadTasks);

        int successes = 0;
        for (int i = 0; i < uploadTasks.size(); i++) {
            try {
                Future<Iterable<RadioPlayerUploadResult>> futureResult = futureResults.take();
                if(!futureResult.isCancelled()) {
                    Iterable<RadioPlayerUploadResult> results = futureResult.get();
                    for (RadioPlayerUploadResult result : results) {
                        if (SUCCESS.equals(result.getUpload().type())) {
                            successes++;
                        }
                    }
                }
            }catch (InterruptedException e) {
                log.record(AdapterLogEntry.warnEntry().withDescription("Radioplayer OD Uploader interrupted waiting for result.").withCause(e));
            } catch (ExecutionException e) {
                log.record(AdapterLogEntry.warnEntry().withDescription("Radioplayer OD Uploader exception retrieving result").withCause(e));
            }
        }

        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        AdapterLogEntry.infoEntry().withDescription("Radioplayer OD Uploader finished in %s, %s/%s successful.", runTime, successes, uploadTasks.size());
    }
}
