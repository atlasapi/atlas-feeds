package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Publisher;


public class InvalidPublisherException extends RuntimeException {

    private static final long serialVersionUID = 4696584586788391154L;

    private Publisher publisher;
    
    public InvalidPublisherException(String message) {
        super(message);
    }
    
    public InvalidPublisherException(Publisher publisher) {
        this("Publisher " + publisher + " not valid");
        this.publisher = publisher;
    }
    
    public Publisher getPublisher() {
        return publisher;
    }
}
