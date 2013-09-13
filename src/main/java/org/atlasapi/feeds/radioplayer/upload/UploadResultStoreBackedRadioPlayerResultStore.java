package org.atlasapi.feeds.radioplayer.upload;

import java.util.List;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;
import org.joda.time.LocalDate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class UploadResultStoreBackedRadioPlayerResultStore implements RadioPlayerUploadResultStore {

    private final FileUploadResultStore backingStore;

    public UploadResultStoreBackedRadioPlayerResultStore(FileUploadResultStore backingStore) {
        this.backingStore = backingStore;
    }
    
    @Override
    public void record(RadioPlayerUploadResult result) {
        backingStore.store(String.format("%s-%s-%s", result.getService().getRadioplayerId(), result.getType().name(), result.getDay().toString("yyyy-MM-dd")), result.getUpload());
    }

    @Override
    public Iterable<FileUploadResult> resultsFor(FileType fileType, String remoteServiceId, RadioPlayerService service, LocalDate day) {
        return backingStore.result(remoteServiceId, String.format("%s-%s", service.getRadioplayerId(), fileType.name()));
    }

    @Override
    public List<FileUploadResult> allUnknownResults(final String remoteServiceId) {
        return ImmutableList.copyOf(Iterables.filter(
                backingStore.results(remoteServiceId),
                FileUploadResult.UNKNOWN_REMOTE_RESULT
        ));
    }

}
