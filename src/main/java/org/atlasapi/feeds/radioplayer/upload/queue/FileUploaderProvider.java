package org.atlasapi.feeds.radioplayer.upload.queue;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.joda.time.DateTime;


public interface FileUploaderProvider {

    FileUploader get(DateTime uploadTime, FileType type);
    
    UploadService serviceKey();
}
