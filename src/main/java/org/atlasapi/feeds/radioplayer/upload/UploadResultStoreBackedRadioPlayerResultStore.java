package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;
import org.joda.time.LocalDate;

public class UploadResultStoreBackedRadioPlayerResultStore implements RadioPlayerUploadResultStore {

    private final FileUploadResultStore backingStore;

    public UploadResultStoreBackedRadioPlayerResultStore(FileUploadResultStore backingStore) {
        this.backingStore = backingStore;
    }
    
    @Override
    public void record(RadioPlayerUploadResult result) {
        backingStore.store(String.format("%s-%s",result.service().getRadioplayerId(), result.day().toString("yyyy-MM-dd")), result.getUpload());
    }

    @Override
    public Iterable<FileUploadResult> resultsFor(String remoteServiceId, RadioPlayerService service, LocalDate day) {
        return backingStore.result(remoteServiceId, String.valueOf(service.getRadioplayerId()));
    }

}
