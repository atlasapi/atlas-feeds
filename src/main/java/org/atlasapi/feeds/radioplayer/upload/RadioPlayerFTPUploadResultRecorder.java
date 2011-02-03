package org.atlasapi.feeds.radioplayer.upload;


public interface RadioPlayerFTPUploadResultRecorder {

    <T extends FTPUploadResult> void  record(T result);

}
