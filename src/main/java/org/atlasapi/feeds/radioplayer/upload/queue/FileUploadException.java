package org.atlasapi.feeds.radioplayer.upload.queue;


public class FileUploadException extends Exception {

    private static final long serialVersionUID = 4496104915114107699L;

    public FileUploadException(String message) {
        super(message);
    }
    
    public FileUploadException(Throwable t) {
        super(t);
    }
    
    public FileUploadException(String message, Throwable t) {
        super(message, t);
    }
}
