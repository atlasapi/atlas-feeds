package org.atlasapi.feeds.youview.tasks;


public interface Destination {

    DestinationType type();
    
    public static enum DestinationType {
        
        YOUVIEW,
        RADIOPLAYER,
        ;
    }
}
