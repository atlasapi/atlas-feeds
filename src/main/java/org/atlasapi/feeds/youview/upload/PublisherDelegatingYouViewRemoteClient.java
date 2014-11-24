package org.atlasapi.feeds.youview.upload;

import java.util.Map;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;


public class PublisherDelegatingYouViewRemoteClient implements YouViewClient {

    private final Map<Publisher, YouViewClient> clients;
    
    public PublisherDelegatingYouViewRemoteClient(Map<Publisher, YouViewClient> clients) {
        this.clients = ImmutableMap.copyOf(clients);
    }

    @Override
    public void upload(Content content) {
        YouViewClient delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.upload(content);
    }

    @Override
    public void sendDeleteFor(Content content) {
        YouViewClient delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.sendDeleteFor(content);
    }

    @Override
    public void checkRemoteStatusOf(Task transaction) {
        YouViewClient delegate = fetchDelegateOrThrow(transaction.publisher());
        delegate.checkRemoteStatusOf(transaction);
    }

    @Override
    public void revoke(Content content) {
        YouViewClient delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.revoke(content);
    }

    @Override
    public void unrevoke(Content content) {
        YouViewClient delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.unrevoke(content);
    }
    
    private YouViewClient fetchDelegateOrThrow(Publisher publisher) {
        YouViewClient delegate = clients.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate;
    }

}
