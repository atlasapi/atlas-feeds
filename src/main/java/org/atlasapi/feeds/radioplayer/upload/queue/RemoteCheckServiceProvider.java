package org.atlasapi.feeds.radioplayer.upload.queue;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.joda.time.DateTime;


public interface RemoteCheckServiceProvider {

    RemoteCheckService get(DateTime uploadTime, FileType type);
    
    UploadService remoteService();
}
