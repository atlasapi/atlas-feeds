package org.atlasapi.feeds.radioplayer.health;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadResultStore;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.joda.time.LocalDate;

import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerUploadSummaryHealthProbe implements HealthProbe {
    
    private final String remoteServiceId;
    private final RadioPlayerService service;
    private final String title;
    private final RadioPlayerUploadResultStore store;
    private final Clock clock;
    private final DayRangeGenerator dayRangeGenerator;
    private final StateChecker stateChecker;
    
    public RadioPlayerUploadSummaryHealthProbe(String remoteServiceId, RadioPlayerService service, RadioPlayerUploadResultStore store, 
            Clock clock, DayRangeGenerator dayRangeGenerator, StateChecker stateChecker) {
        this.remoteServiceId = checkNotNull(remoteServiceId);
        this.service = checkNotNull(service);
        this.title = createTitle(remoteServiceId, service);
        this.store = checkNotNull(store);
        this.clock = checkNotNull(clock);
        this.dayRangeGenerator = checkNotNull(dayRangeGenerator);
        this.stateChecker = checkNotNull(stateChecker);
    }

    /**
     * As the title is embedded into html, the title can contain a link through to the
     * health probe containing the results for a single remote service / RP service combination
     */
    private String createTitle(String remoteServiceId, RadioPlayerService service) {
        return String.format("<a href='/system/health/ukrp-%s-%s'>%d - %s</a>", remoteServiceId, service.getName(), service.getRadioplayerId(), service.getName());
    }

    @Override
    public ProbeResult probe() throws Exception {
        ProbeResult result = new ProbeResult(title);
        
        DayRange dayRange = dayRangeGenerator.generate(clock.now().toLocalDate());
        
        UpdateProgress piResult = UpdateProgress.START;
        UpdateProgress odResult = UpdateProgress.START;
        for (LocalDate day : dayRange) {
            piResult = piResult.reduce(processDay(day, PI, store.resultsFor(PI, remoteServiceId, service, day)));
            odResult = odResult.reduce(processDay(day, OD, store.resultsFor(OD, remoteServiceId, service, day)));
        }
        result.addEntry(entryFor(piResult, PI));
        result.addEntry(entryFor(odResult, OD));
        
        return result;
    }

    private ProbeResultEntry entryFor(UpdateProgress result, FileType fileType) {
        ProbeResultType type;
        if (result.getFailures() > 0) {
            type = ProbeResultType.FAILURE;
        } else {
            type = ProbeResultType.SUCCESS;
        }
        return new ProbeResultEntry(type, fileType.name(), String.format("%s/%s succeeded", result.getProcessed(), result.getTotalProgress()));
    }

    private UpdateProgress processDay(LocalDate day, FileType type, Iterable<? extends FileUploadResult> results) {
        if (Iterables.isEmpty(results)) {
            return UpdateProgress.START;
        }
        List<? extends FileUploadResult> dateOrderedResults = stateChecker.orderByDate(results);
        ProbeResultType entryResultType = stateChecker.entryResultType(stateChecker.mostRecentSuccess(dateOrderedResults), dateOrderedResults.get(0), day, type, service);
        switch (entryResultType) {
        case FAILURE:
            return UpdateProgress.FAILURE;
        case INFO:
            return UpdateProgress.START;
        case SUCCESS:
            return UpdateProgress.SUCCESS;
        default:
            return UpdateProgress.START;
        
        }
    }
    
    @Override
    public String title() {
        return title;
    }

    @Override
    public String slug() {
        return String.format("ukrp-summary-%s-%s", remoteServiceId, service.getName());
    }
}
