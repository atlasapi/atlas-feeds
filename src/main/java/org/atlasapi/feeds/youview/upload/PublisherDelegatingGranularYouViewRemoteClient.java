package org.atlasapi.feeds.youview.upload;

import java.util.Map;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.feeds.youview.upload.granular.GranularYouViewService;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;


public class PublisherDelegatingGranularYouViewRemoteClient implements GranularYouViewService {

    private final Map<Publisher, GranularYouViewService> clients;
    
    public PublisherDelegatingGranularYouViewRemoteClient(Map<Publisher, GranularYouViewService> clients) {
        this.clients = ImmutableMap.copyOf(clients);
    }

    @Override
    public void uploadContent(Content content) {
        GranularYouViewService delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.uploadContent(content);
    }

    @Override
    public void uploadVersion(ItemAndVersion versionHierarchy, String versionCrid) {
        GranularYouViewService delegate = fetchDelegateOrThrow(versionHierarchy.item().getPublisher());
        delegate.uploadVersion(versionHierarchy, versionCrid);
    }

    @Override
    public void uploadBroadcast(ItemBroadcastHierarchy broadcastHierarchy, String broadcastImi) {
        GranularYouViewService delegate = fetchDelegateOrThrow(broadcastHierarchy.item().getPublisher());
        delegate.uploadBroadcast(broadcastHierarchy, broadcastImi);
    }

    @Override
    public void uploadOnDemand(ItemOnDemandHierarchy onDemandHierarchy, String onDemandImi) {
        GranularYouViewService delegate = fetchDelegateOrThrow(onDemandHierarchy.item().getPublisher());
        delegate.uploadOnDemand(onDemandHierarchy, onDemandImi);
    }

    @Override
    public void sendDeleteFor(Content content, TVAElementType type, String elementId) {
        GranularYouViewService delegate = fetchDelegateOrThrow(content.getPublisher());
        delegate.sendDeleteFor(content, type, elementId);
    }

    @Override
    public void checkRemoteStatusOf(Task transaction) {
        GranularYouViewService delegate = fetchDelegateOrThrow(transaction.publisher());
        delegate.checkRemoteStatusOf(transaction);
    }
    
    private GranularYouViewService fetchDelegateOrThrow(Publisher publisher) {
        GranularYouViewService delegate = clients.get(publisher);
        if (delegate == null) {
            throw new InvalidPublisherException(publisher);
        }
        return delegate;
    }

}
