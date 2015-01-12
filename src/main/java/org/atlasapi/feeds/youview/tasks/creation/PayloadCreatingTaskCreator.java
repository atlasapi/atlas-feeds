package org.atlasapi.feeds.youview.tasks.creation;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.payload.PayloadCreator;
import org.atlasapi.feeds.youview.payload.PayloadGenerationException;
import org.atlasapi.feeds.youview.tasks.Action;
import org.atlasapi.feeds.youview.tasks.Status;
import org.atlasapi.feeds.youview.tasks.TVAElementType;
import org.atlasapi.feeds.youview.tasks.Task;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Series;


public class PayloadCreatingTaskCreator implements TaskCreator {

    private final PayloadCreator payloadCreator;

    public PayloadCreatingTaskCreator(PayloadCreator payloadCreator) {
        this.payloadCreator = checkNotNull(payloadCreator);
    }

    @Override
    public Task create(String contentCrid, Content content, Action action) throws TaskCreationException {
        try {
            return Task.builder()
                    .withAction(action)
                    .withContent(content.getCanonicalUri())
                    .withPublisher(content.getPublisher())
                    .withElementType(contentTypeFrom(content))
                    .withElementId(contentCrid)
                    .withPayload(payloadCreator.createFrom(content))
                    .withStatus(Status.NEW)
                    .build();
        } catch (PayloadGenerationException e) {
            throw new TaskCreationException(e);
        }
    }

    private TVAElementType contentTypeFrom(Content content) {
        if (content instanceof Brand) {
            return TVAElementType.BRAND;
        }
        if (content instanceof Series) {
            return TVAElementType.SERIES;
        }
        return TVAElementType.ITEM;
    }

    @Override
    public Task create(String versionCrid, ItemAndVersion versionHierarchy, Action action) throws TaskCreationException {
        try {
            return Task.builder()
                    .withAction(action)
                    .withContent(versionHierarchy.item().getCanonicalUri())
                    .withPublisher(versionHierarchy.item().getPublisher())
                    .withElementType(TVAElementType.VERSION)
                    .withElementId(versionCrid)
                    .withPayload(payloadCreator.createFrom(versionCrid, versionHierarchy))
                    .withStatus(Status.NEW)
                    .build();
        } catch (PayloadGenerationException e) {
            throw new TaskCreationException(e);
        }
    }

    @Override
    public Task create(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Action action) throws TaskCreationException {
        try {
            return Task.builder()
                    .withAction(action)
                    .withContent(broadcastHierarchy.item().getCanonicalUri())
                    .withPublisher(broadcastHierarchy.item().getPublisher())
                    .withElementType(TVAElementType.BROADCAST)
                    .withElementId(broadcastImi)
                    .withPayload(payloadCreator.createFrom(broadcastImi, broadcastHierarchy))
                    .withStatus(Status.NEW)
                    .build();
        } catch (PayloadGenerationException e) {
            throw new TaskCreationException(e);
        }
    }

    @Override
    public Task create(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Action action) throws TaskCreationException {
        try {
            return Task.builder()
                    .withAction(action)
                    .withContent(onDemandHierarchy.item().getCanonicalUri())
                    .withPublisher(onDemandHierarchy.item().getPublisher())
                    .withElementType(TVAElementType.ONDEMAND)
                    .withElementId(onDemandImi)
                    .withPayload(payloadCreator.createFrom(onDemandImi, onDemandHierarchy))
                    .withStatus(Status.NEW)
                    .build();
        } catch (PayloadGenerationException e) {
            throw new TaskCreationException(e);
        }
    }

}
