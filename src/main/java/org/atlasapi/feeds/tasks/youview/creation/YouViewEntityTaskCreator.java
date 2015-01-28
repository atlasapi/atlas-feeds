package org.atlasapi.feeds.tasks.youview.creation;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.youview.UnexpectedContentTypeException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;


public class YouViewEntityTaskCreator implements TaskCreator {

    @Override
    public Task taskFor(String contentCrid, Content content, Action action) {
        Destination destination = new YouViewDestination(content.getCanonicalUri(), contentTypeFrom(content), contentCrid);
        return createTask(content.getPublisher(), action, destination);
    }

    @Override
    public Task taskFor(String versionCrid, ItemAndVersion versionHierarchy, Action action) {
        Destination destination = new YouViewDestination(versionHierarchy.item().getCanonicalUri(), TVAElementType.VERSION, versionCrid);
        return createTask(versionHierarchy.item().getPublisher(), action, destination);
    }

    @Override
    public Task taskFor(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Action action) {
        Destination destination = new YouViewDestination(broadcastHierarchy.item().getCanonicalUri(), TVAElementType.BROADCAST, broadcastImi);
        return createTask(broadcastHierarchy.item().getPublisher(), action, destination);
    }

    @Override
    public Task taskFor(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Action action) {
        Destination destination = new YouViewDestination(onDemandHierarchy.item().getCanonicalUri(), TVAElementType.ONDEMAND, onDemandImi);
        return createTask(onDemandHierarchy.item().getPublisher(), action, destination);
    }

    private Task createTask(Publisher publisher, Action action, Destination destination) {
        return Task.builder()
                .withAction(action)
                .withDestination(destination)
                .withPublisher(publisher)
                .withStatus(Status.NEW)
                .build();
    }

    private TVAElementType contentTypeFrom(Content content) {
        if (content instanceof Brand) {
            return TVAElementType.BRAND;
        }
        if (content instanceof Series) {
            return TVAElementType.SERIES;
        }
        if (content instanceof Item) {
            return TVAElementType.ITEM;
        }
        throw new UnexpectedContentTypeException(content);
    }
}
