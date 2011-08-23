package org.atlasapi.feeds.interlinking.delta;

import static com.google.common.base.Preconditions.checkNotNull;
import nu.xom.Document;

import org.joda.time.DateTime;

import com.metabroadcast.common.base.Maybe;

public abstract class InterlinkingDelta {

    public static final InterlinkingDelta deltaFor(Maybe<Document> document, DateTime lastUpdated) {
        return document.hasValue() ? new ExistingDelta(document.requireValue(), lastUpdated) : new NewDelta(); 
    }

    public static final InterlinkingDelta deltaFor(Document document, DateTime lastUpdated) {
        return new ExistingDelta(document, lastUpdated); 
    }
    
    public abstract Document document();
    public abstract DateTime lastUpdated();
    public abstract boolean exists();
    
    private static class ExistingDelta extends InterlinkingDelta {
        
        private final Document document;
        private final DateTime lastUpdated;
        
        public ExistingDelta(Document document, DateTime lastUpdated) {
            this.document = checkNotNull(document);
            this.lastUpdated = checkNotNull(lastUpdated);
        }
        
        @Override
        public Document document() {
            return document;
        }
        
        @Override
        public DateTime lastUpdated() {
            return lastUpdated;
        }

        @Override
        public boolean exists() {
            return true;
        }
        
        @Override
        public String toString() {
            return "Interlinking delta from " + lastUpdated.toString();
        }
    }
    
    private static class NewDelta extends InterlinkingDelta {

        @Override
        public Document document() {
            throw new NullPointerException();
        }

        @Override
        public DateTime lastUpdated() {
            throw new NullPointerException();
        }

        @Override
        public boolean exists() {
            return false;
        }
        
        @Override
        public String toString() {
            return "New Interlinking Delta";
        }
        
    }
}
