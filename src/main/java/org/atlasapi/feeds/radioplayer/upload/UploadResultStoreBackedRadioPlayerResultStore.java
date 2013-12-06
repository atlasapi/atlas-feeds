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
        backingStore.store(keyFor(result.getService(), result.getType(), result.getDay()), result.getUpload());
    }

    @Override
    public Iterable<FileUploadResult> resultsFor(FileType fileType, String remoteServiceId, RadioPlayerService service, LocalDate day) {
        return backingStore.result(remoteServiceId, keyFor(service, fileType, day));
    }

    @Override
    public List<FileUploadResult> allSuccessfulResults(final String remoteServiceId) {
        return ImmutableList.copyOf(Iterables.filter(
                backingStore.results(remoteServiceId),
                FileUploadResult.SUCCESSFUL
        ));
    }
    
    private String keyFor(RadioPlayerService service, FileType type, LocalDate day) {
        return String.format("%s-%s-%s", service.getRadioplayerId(), type.name(), day.toString("yyyy-MM-dd"));
    }

}
