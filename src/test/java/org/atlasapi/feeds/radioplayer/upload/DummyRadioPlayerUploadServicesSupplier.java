package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.upload.FileUploadService;
import org.joda.time.DateTime;

/**
 * Dummy implementation of the RadioPlayerUploadServicesSupplier, that simply returns the Iterable<FileUploadService>
 * passed in to the construct whenever the get method is called.
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public class DummyRadioPlayerUploadServicesSupplier extends RadioPlayerUploadServicesSupplier {

    private final Iterable<FileUploadService> uploadServices;

    public DummyRadioPlayerUploadServicesSupplier(Iterable<FileUploadService> uploadServices) {
        super(null, null, null, null, null);
        this.uploadServices = uploadServices;
    }

    @Override
    Iterable<FileUploadService> get(DateTime uploadTime, FileType type) {
        return uploadServices;
    }

}
