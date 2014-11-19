package org.atlasapi.feeds.youview.upload;

import java.util.Map;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.feeds.youview.transactions.Transaction;
import org.atlasapi.feeds.youview.transactions.TransactionStatus;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;


public class PublisherDelegatingYouViewRemoteClient implements YouViewRemoteClient {

    private final Map<Publisher, YouViewRemoteClient> clients;
    
    public PublisherDelegatingYouViewRemoteClient(Map<Publisher, YouViewRemoteClient> clients) {
        this.clients = ImmutableMap.copyOf(clients);
    }

    @Override
    public Transaction upload(Content content) {
        Publisher publisher = content.getPublisher();
        YouViewRemoteClient delegate = clients.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate.upload(content);
    }

    @Override
    public boolean sendDeleteFor(Content content) {
        Publisher publisher = content.getPublisher();
        YouViewRemoteClient delegate = clients.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate.sendDeleteFor(content);
    }

    @Override
    public TransactionStatus checkRemoteStatusOf(Transaction transaction) {
        Publisher publisher = transaction.publisher();
        YouViewRemoteClient delegate = clients.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate.checkRemoteStatusOf(transaction);
    }

}
