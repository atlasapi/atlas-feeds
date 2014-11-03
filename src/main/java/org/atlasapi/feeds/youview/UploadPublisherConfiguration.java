package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Objects;
import com.metabroadcast.common.security.UsernameAndPassword;


public class UploadPublisherConfiguration {

    private final Publisher publisher;
    private final String url;
    private final UsernameAndPassword credentials;
    private final int chunkSize;
    
    public UploadPublisherConfiguration(Publisher publisher, String url, UsernameAndPassword credentials, int chunkSize) {
        this.publisher = checkNotNull(publisher);
        this.url = checkNotNull(url);
        this.credentials = checkNotNull(credentials);
        this.chunkSize = chunkSize;
    }
    
    public Publisher publisher() {
        return publisher;
    }
    
    public String url() {
        return url;
    }
    
    public UsernameAndPassword credentials() {
        return credentials;
    }
    
    public int chunkSize() {
        return chunkSize;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(UploadPublisherConfiguration.class)
                .add("publisher", publisher)
                .add("url", url)
                .add("credentials", credentials)
                .add("chunkSize", chunkSize)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(publisher);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof UploadPublisherConfiguration) {
            UploadPublisherConfiguration other = (UploadPublisherConfiguration) that;
            return publisher.equals(other.publisher);
        }
        
        return false;
    }
}
