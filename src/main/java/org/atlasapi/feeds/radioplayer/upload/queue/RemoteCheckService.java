package org.atlasapi.feeds.radioplayer.upload.queue;


public interface RemoteCheckService {

    RemoteCheckResult check(RemoteCheckTask task) throws RemoteCheckException;

    UploadService remoteService();
}
