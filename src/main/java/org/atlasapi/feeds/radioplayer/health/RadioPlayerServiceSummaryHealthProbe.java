package org.atlasapi.feeds.radioplayer.health;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.radioplayer.health.FileHistoryOutputter.createJsToggleCode;
import static org.atlasapi.feeds.radioplayer.health.RadioPlayerUploadHealthProbe.columnsFrom;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.FileHistoryStore;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerServiceSummaryHealthProbe implements HealthProbe {
    
    private static final Predicate<ProbeResultType> IS_NOT_SUCCESS = new Predicate<ProbeResultType>() {
        @Override
        public boolean apply(ProbeResultType input) {
            return !input.equals(ProbeResultType.SUCCESS);
        }
    };
    private final UploadService uploadService;
    private final RadioPlayerService service;
    private final String title;
    private final Clock clock;
    private final DayRangeGenerator dayRangeGenerator;
    private final FileHistoryStore fileStore;
    private final ResultTypeCalculator resultTypeCalculator;
    
    public RadioPlayerServiceSummaryHealthProbe(UploadService uploadService, RadioPlayerService service, FileHistoryStore fileStore,
            Clock clock, DayRangeGenerator dayRangeGenerator, ResultTypeCalculator resultTypeCalculator) {
        this.uploadService = checkNotNull(uploadService);
        this.service = checkNotNull(service);
        this.fileStore = checkNotNull(fileStore);
        this.title = createTitle(uploadService, service);
        this.clock = checkNotNull(clock);
        this.dayRangeGenerator = checkNotNull(dayRangeGenerator);
        this.resultTypeCalculator = checkNotNull(resultTypeCalculator);
    }

    /**
     * As the title String is embedded into html, the title can contain a link through to the
     * health probe containing the results for a single remote service / RP service combination.
     * This title will also contain a JS script that adds toggle buttons for the various result modes,
     * if it is the probe for the first RP service. 
     */
    private String createTitle(UploadService uploadService, RadioPlayerService service) {
        String title = String.format(
                "<a href='/feeds/ukradioplayer/health/%s/services/%d'>%d - %s</a>", 
                uploadService.name().toLowerCase(), 
                service.getRadioplayerId(), 
                service.getRadioplayerId(), 
                service.getName()
        );
        
        if (service.equals(Iterables.getFirst(RadioPlayerServices.services, null))) {
            return title + createJsToggleCode();
        }
        
        return title;
    }

    @Override
    public ProbeResult probe() throws Exception {
        ProbeResult result = new ProbeResult(title);
        
        DayRange dayRange = dayRangeGenerator.generate(clock.now().toLocalDate());
        
        UpdateProgress piResult = UpdateProgress.START;
        UpdateProgress odResult = UpdateProgress.START;
        int totalFiles = 0;
        
        for (LocalDate day : dayRange) {
            totalFiles += 1;
            piResult = piResult.reduce(processDay(day, fileStore.fetch(new RadioPlayerFile(uploadService, service, PI, day))));
            odResult = odResult.reduce(processDay(day, fileStore.fetch(new RadioPlayerFile(uploadService, service, OD, day))));
        }
        
        ProbeResultEntry piEntry = entryFor(piResult, PI, totalFiles);
        ProbeResultEntry odEntry = entryFor(odResult, OD, totalFiles);
        
        result.addEntry(generateTableHeaderEntry(calculateHeaderResult(ImmutableSet.of(piEntry.getType(), odEntry.getType()))));
        result.addEntry(piEntry);
        result.addEntry(odEntry);
        
        return result;
    }

    private UpdateProgress processDay(LocalDate day, Optional<FileHistory> file) {
        if (!file.isPresent()) {
            return UpdateProgress.FAILURE;
        }
        FileHistory history = file.get();
        if (history.uploadAttempts().isEmpty()) {
            return UpdateProgress.FAILURE;
        }
        ProbeResultType result = resultTypeCalculator.calculateResultType(history);
        switch (result) {
        case FAILURE:
            return UpdateProgress.FAILURE;
        case SUCCESS:
            return UpdateProgress.SUCCESS;
        case INFO:
        default:
            return null;
        }
    }

    static ProbeResultType calculateHeaderResult(Iterable<ProbeResultType> results) {
        if (Iterables.contains(results, ProbeResultType.FAILURE)) {
            return ProbeResultType.FAILURE;
        }
        if (Iterables.isEmpty(Iterables.filter(results, IS_NOT_SUCCESS))) {
            return ProbeResultType.SUCCESS;
        }
        return ProbeResultType.INFO;
    }

    private ProbeResultEntry generateTableHeaderEntry(ProbeResultType serviceResult) {
        return new ProbeResultEntry(serviceResult, "Type", columnsFrom(ImmutableList.of("Successful", "Failure", "Info")));
    }

    private ProbeResultEntry entryFor(UpdateProgress result, FileType fileType, int totalFiles) {
        ProbeResultType type;
        if (result.getFailures() > 0) {
            type = ProbeResultType.FAILURE;
        } else {
            type = ProbeResultType.SUCCESS;
        }
        return new ProbeResultEntry(type, fileType.name(), columnsFrom(ImmutableList.of(
                String.valueOf(result.getProcessed()), 
                String.valueOf(result.getFailures()), 
                String.valueOf(totalFiles - result.getTotalProgress())
        )));
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String slug() {
        return String.format("ukrp-summary-%s-%s", uploadService.name().toLowerCase(), service.getName());
    }
}
