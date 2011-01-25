package org.atlasapi.feeds.radioplayer.upload;

import org.apache.commons.net.ftp.FTPClient;


public interface FTPUpload {

    FTPUploadResult upload(FTPClient client, String filename, byte[] fileData);
    
}
