package org.atlasapi.feeds.tasks.youview.creation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.feeds.youview.UnexpectedContentTypeException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.feeds.youview.nitro.NitroIdGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;

import com.metabroadcast.common.time.Clock;


public class YouViewEntityTaskCreator implements TaskCreator {

    private static final String NO_PAYLOAD_ERROR = "Can't create status=NEW without payload";
    private final Clock clock;

    public YouViewEntityTaskCreator(Clock clock) {
        this.clock = checkNotNull(clock);
    }

    @Override
    public Task deleteFor(String contentCrid, Content content) {
        return taskFor(contentCrid, content, null, Action.DELETE, Status.NEW);
    }

    @Override
    public Task deleteFor(String versionCrid, ItemAndVersion versionHierarchy) {
        return taskFor(versionCrid, versionHierarchy, null, Action.DELETE, Status.NEW);
    }

    @Override
    public Task deleteFor(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy) {
        return taskFor(broadcastImi, broadcastHierarchy, null, Action.DELETE, Status.NEW);
    }

    @Override
    public Task deleteFor(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy) {
        return taskFor(onDemandImi, onDemandHierarchy, null, Action.DELETE, Status.NEW);
    }

    @Override
    public Task deleteFor(String channelCrid, Channel channel) {
        return taskFor(channelCrid, channel, null, Action.DELETE, Status.NEW);
    }

    @Override
    public Task taskFor(String contentCrid, Content content, Action action, Status status) {
        checkArgument(status != Status.NEW, NO_PAYLOAD_ERROR);
        return taskFor(contentCrid, content, null, action, status);
    }

    @Override
    public Task taskFor(String versionCrid, ItemAndVersion versionHierarchy, Action action, Status status) {
        checkArgument(status != Status.NEW, NO_PAYLOAD_ERROR);
        return taskFor(versionCrid, versionHierarchy, null, action, status);
    }

    @Override
    public Task taskFor(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Action action, Status status) {
        checkArgument(status != Status.NEW, NO_PAYLOAD_ERROR);
        return taskFor(broadcastImi, broadcastHierarchy, null, action, status);
    }

    @Override
    public Task taskFor(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Action action, Status status) {
        checkArgument(status != Status.NEW, NO_PAYLOAD_ERROR);
        return taskFor(onDemandImi, onDemandHierarchy, null, action, status);
    }

    @Override
    public Task taskFor(String channelCrid, Channel channel, Action action, Status status) {
        checkArgument(status != Status.NEW, NO_PAYLOAD_ERROR);
        return taskFor(channelCrid, channel, null, action, status);
    }

    @Override
    public Task taskFor(String contentCrid, Content content, Payload payload, Action action) {
        return taskFor(contentCrid, content, checkNotNull(payload), action, Status.NEW);
    }

    @Override
    public Task taskFor(String versionCrid, ItemAndVersion versionHierarchy, Payload payload, Action action) {
        return taskFor(versionCrid, versionHierarchy, checkNotNull(payload), action, Status.NEW);
    }

    @Override
    public Task taskFor(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Payload payload, Action action) {
        return taskFor(broadcastImi, broadcastHierarchy, checkNotNull(payload), action, Status.NEW);
    }

    @Override
    public Task taskFor(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Payload payload, Action action) {
        return taskFor(onDemandImi, onDemandHierarchy, checkNotNull(payload), action, Status.NEW);
    }

    @Override
    public Task taskFor(String channelCrid, Channel channel, Payload payload, Action action) {
        return taskFor(channelCrid, channel, checkNotNull(payload), action, Status.NEW);
    }

    private Task taskFor(String contentCrid, Content content, Payload payload, Action action, Status status) {
        TVAElementType type = contentTypeFrom(content);
        Destination destination = new YouViewDestination(content.getCanonicalUri(), type, contentCrid);
        return createTask(content.getId(), content.getPublisher(), payload, action, destination, status);
    }

    private Task taskFor(String versionCrid, ItemAndVersion versionHierarchy, Payload payload, Action action, Status status) {
        Destination destination = new YouViewDestination(versionHierarchy.item().getCanonicalUri(), TVAElementType.VERSION, versionCrid);
        return createTask(versionHierarchy.item().getId(), versionHierarchy.item().getPublisher(), payload, action, destination, status);
    }

    private Task taskFor(String broadcastImi, ItemBroadcastHierarchy broadcastHierarchy, Payload payload, Action action, Status status) {
        Destination destination = new YouViewDestination(broadcastHierarchy.item().getCanonicalUri(), TVAElementType.BROADCAST, broadcastImi);
        return createTask(broadcastHierarchy.item().getId(), broadcastHierarchy.item().getPublisher(), payload, action, destination, status);
    }

    private Task taskFor(String onDemandImi, ItemOnDemandHierarchy onDemandHierarchy, Payload payload, Action action, Status status) {
        Destination destination = new YouViewDestination(onDemandHierarchy.item().getCanonicalUri(), TVAElementType.ONDEMAND, onDemandImi);
        return createTask(onDemandHierarchy.item().getId(), onDemandHierarchy.item().getPublisher(), payload, action, destination, status);
    }

    private Task taskFor(String channelCrid, Channel channel, Payload payload, Action action, Status status) {
        String contentUri = NitroIdGenerator.generateChannelServiceId(channel);
        Destination destination = new YouViewDestination(
                !Strings.isNullOrEmpty(contentUri) ? contentUri : channel.getCanonicalUri(),
                TVAElementType.CHANNEL,
                channelCrid
        );
        return createTask(channel.getId(), channel.getSource(), payload, action, destination, status);
    }

    private Task createTask(
            Long atlasDbId,
            Publisher publisher,
            Payload payload,
            Action action,
            Destination destination,
            Status status) {

        return Task.builder()
                .withAtlasDbId(atlasDbId)
                .withAction(action)
                .withCreated(clock.now())
                .withDestination(destination)
                .withPublisher(publisher)
                .withStatus(status)
                .withPayload(payload)
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
