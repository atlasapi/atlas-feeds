package org.atlasapi.feeds.tasks;


public interface Destination {

    DestinationType type();
    
    public static enum DestinationType {
        
        YOUVIEW,
        RADIOPLAYER,
        ;
    }
}
