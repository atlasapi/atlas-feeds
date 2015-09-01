package org.atlasapi.feeds.tasks;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.tasks.Destination.DestinationType.YOUVIEW;

import com.google.common.base.Objects;


public final class YouViewDestination implements Destination {

    private static final DestinationType TYPE = YOUVIEW;
    private final String contentUri;
    private final TVAElementType elementType;
    private final String elementId;

    public YouViewDestination(String contentUri, TVAElementType elementType, String elementId) {
        this.contentUri = checkNotNull(contentUri);
        this.elementType = checkNotNull(elementType);
        this.elementId = checkNotNull(elementId);
    }

    @Override
    public DestinationType type() {
        return TYPE;
    }

    public String contentUri() {
        return contentUri;
    }

    public TVAElementType elementType() {
        return elementType;
    }

    public String elementId() {
        return elementId;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(YouViewDestination.class)
                .add("type", TYPE)
                .add("contentUri", contentUri)
                .add("elementType", elementType)
                .add("elementId", elementId)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(TYPE, contentUri, elementType, elementId);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        
        if (!(that instanceof YouViewDestination)) {
            return false;
        }
        
        YouViewDestination other = (YouViewDestination) that;
        return contentUri.equals(other.contentUri)
                && elementType.equals(other.elementType)
                && elementId.equals(other.elementId); 
    }
}
