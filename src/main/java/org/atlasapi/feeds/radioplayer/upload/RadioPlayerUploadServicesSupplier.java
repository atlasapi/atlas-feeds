package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.upload.FileUploadService;
import org.joda.time.DateTime;

public interface RadioPlayerUploadServicesSupplier {

    public abstract Iterable<FileUploadService> get(DateTime uploadTime, FileType type);

}