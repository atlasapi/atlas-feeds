package org.atlasapi.feeds.youview.tasks.creation;


public class TaskCreationException extends Exception {

    private static final long serialVersionUID = 1L;

    public TaskCreationException(String message) {
        super(message);
    }
    
    public TaskCreationException(Exception e) {
        super(e);
    }
    
    public TaskCreationException(String message, Exception e) {
        super(message, e);
    }
}
