package org.atlasapi.feeds.radioplayer.upload.queue;


public enum UploadService {

    S3,
    HTTPS
    ;
    
    // avoids having to deal with IllegalArgumentExceptions for invalid values
    // when using Enum.valueOf
    public static UploadService fromString(String uploadServiceName) {
        for (UploadService service : UploadService.values()) {
            if (service.name().equalsIgnoreCase(uploadServiceName)) {
                return service;
            }
        }
        return null;
    }
}
