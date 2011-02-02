package org.atlasapi.feeds.radioplayer.upload;


public interface FTPUploadResultRecorder {

    void record(Iterable<? extends FTPUploadResult> result);

}
