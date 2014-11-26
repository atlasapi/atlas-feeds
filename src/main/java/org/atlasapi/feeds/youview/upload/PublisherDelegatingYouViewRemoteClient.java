package org.atlasapi.feeds.youview.upload;

import java.util.Map;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;


public class PublisherDelegatingYouViewRemoteClient implements YouViewService {

    private final Map<Publisher, YouViewService> clients;
    
    public PublisherDelegatingYouViewRemoteClient(Map<Publisher, YouViewService> clients) {
        this.clients = ImmutableMap.copyOf(clients);
    }

    @Override
    public void upload(Content content) {
        YouViewService delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.upload(content);
    }

    @Override
    public void sendDeleteFor(Content content) {
        YouViewService delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.sendDeleteFor(content);
    }

    @Override
    public void checkRemoteStatusOf(Task transaction) {
        YouViewService delegate = fetchDelegateOrThrow(transaction.publisher());
        delegate.checkRemoteStatusOf(transaction);
    }

    @Override
    public void revoke(Content content) {
        YouViewService delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.revoke(content);
    }

    @Override
    public void unrevoke(Content content) {
        YouViewService delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.unrevoke(content);
    }
    
    private YouViewService fetchDelegateOrThrow(Publisher publisher) {
        YouViewService delegate = clients.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate;
    }

}
