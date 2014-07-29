package org.atlasapi.feeds.radioplayer.health;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.radioplayer.health.FileHistoryOutputter.buildRemoteCheckInfo;
import static org.atlasapi.feeds.radioplayer.health.FileHistoryOutputter.buildUploadInfo;
import static org.atlasapi.feeds.radioplayer.health.FileHistoryOutputter.createJsToggleCode;
import static org.atlasapi.feeds.radioplayer.health.RadioPlayerServiceSummaryHealthProbe.calculateHeaderResult;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.FileHistoryStore;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

public class RadioPlayerUploadHealthProbe implements HealthProbe {

    private static final Function<ProbeResultEntry, ProbeResultType> TO_RESULT_TYPE = new Function<ProbeResultEntry, ProbeResultType>() {
        @Override
        public ProbeResultType apply(ProbeResultEntry input) {
            return input.getType();
        }
    };
    
    private final Clock clock;
    private final UploadService uploadService;
    private final FileHistoryStore fileStore;
    private final RadioPlayerService service;
    private final DayRangeGenerator rangeGenerator;
    private final ResultTypeCalculator resultTypeCalculator;

    public RadioPlayerUploadHealthProbe(Clock clock, UploadService uploadService, FileHistoryStore fileStore, RadioPlayerService service, 
            DayRangeGenerator dayRangeGenerator, ResultTypeCalculator resultTypeCalculator) {
        this.clock = checkNotNull(clock);
        this.uploadService = checkNotNull(uploadService);
        this.fileStore = checkNotNull(fileStore);
        this.service = checkNotNull(service);
        this.rangeGenerator = checkNotNull(dayRangeGenerator);
        this.resultTypeCalculator = checkNotNull(resultTypeCalculator);
    }

    @Override
    public ProbeResult probe() {
        
        DayRange dayRange = rangeGenerator.generate(clock.now().toLocalDate());
        
        ImmutableList.Builder<ProbeResultEntry> entries = ImmutableList.<ProbeResultEntry>builder();
        for (LocalDate day : dayRange) {
            entries.add(entryFor(new RadioPlayerFile(uploadService, service, PI, day)));
            entries.add(entryFor(new RadioPlayerFile(uploadService, service, OD, day)));
        }

        return buildProbeResult(entries.build());
    }

    private ProbeResult buildProbeResult(List<ProbeResultEntry> entries) {
        ProbeResult result = new ProbeResult(service.getName());
        result.addEntry(generateTableHeaderEntry(calculateHeaderResult(Iterables.transform(entries, TO_RESULT_TYPE))));
        for (ProbeResultEntry entry : entries) {
            result.addEntry(entry);
        }
        return result;
    }

    private ProbeResultEntry generateTableHeaderEntry(ProbeResultType serviceResult) {
        return new ProbeResultEntry(serviceResult, "Details" + createJsToggleCode(), columnsFrom(ImmutableList.of("Upload", "Remote Check")));
    }
    
    static String columnsFrom(List<String> columns) {
        StringBuilder columnStr = new StringBuilder();
        columnStr.append(columns.get(0));
        for (String column : Iterables.skip(columns, 1)) {
            columnStr.append("<td>");
            columnStr.append(column);
            columnStr.append("</td>");
        }
        return columnStr.toString();
    }

    private ProbeResultEntry entryFor(RadioPlayerFile file) {
        
        Optional<FileHistory> fileRecord = fileStore.fetch(file);
        
        FileHistory fileHistory;
        if (!fileRecord.isPresent()) {
            fileHistory = createAndStoreNewFile(file);
        } else {
            fileHistory = fileRecord.get();
        }
        
        if (fileHistory.uploadAttempts().isEmpty()) {
            return new ProbeResultEntry(
                    ProbeResultType.FAILURE, 
                    FileHistoryOutputter.printFileDetails(fileHistory.file()), 
                    columnsFrom(ImmutableList.of(
                            "No Uploads Made", 
                            "N/A"
                    ))
            );
        } else {
            ProbeResultType resultType = resultTypeCalculator.calculateResultType(fileHistory);
            UploadAttempt latestAttempt = fileHistory.getLatestUpload();
            return new ProbeResultEntry(
                    resultType, 
                    FileHistoryOutputter.printFileDetails(fileHistory.file()), 
                    columnsFrom(ImmutableList.of(
                            buildUploadInfo(latestAttempt), 
                            buildRemoteCheckInfo(latestAttempt)
                    ))
            );
        }
    }

    /**
     * creates a new FileHistory object for the appropriate file, and stores it
     */
    private FileHistory createAndStoreNewFile(RadioPlayerFile file) {
        FileHistory fileHistory = new FileHistory(file);
        fileStore.store(fileHistory);
        return fileHistory;
    }

    @Override
    public String title() {
        return service.getName();
    }

    @Override
    public String slug() {
        return String.format("ukrp-%s-%s", uploadService.name().toLowerCase(), service.getName());
    }

}
