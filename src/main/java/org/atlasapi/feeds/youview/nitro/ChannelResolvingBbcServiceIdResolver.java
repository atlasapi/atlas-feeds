package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.youview.NoChannelFoundException;
import org.atlasapi.feeds.youview.NoSuchChannelAliasException;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.base.Maybe;


public final class ChannelResolvingBbcServiceIdResolver implements BbcServiceIdResolver {
    
    private static final String BBC_SID_NAMESPACE = "bbc:service:id";
    private static final Predicate<Alias> IS_BBC_SID_ALIAS = new Predicate<Alias>() {
        @Override
        public boolean apply(Alias input) {
            return BBC_SID_NAMESPACE.equals(input.getNamespace());
        }
    };
    
    private final ChannelResolver channelResolver;

    public ChannelResolvingBbcServiceIdResolver(ChannelResolver channelResolver) {
        this.channelResolver = checkNotNull(channelResolver);
    }

    @Override
    public String resolveSId(Broadcast broadcast) {
        return resolveServiceId(broadcast.getBroadcastOn());
    }

    private String resolveServiceId(String channelUri) {
        Maybe<Channel> resolved = channelResolver.fromUri(channelUri);
        if (resolved.isNothing()) {
            throw new NoChannelFoundException(channelUri);
        }
        Channel channel = resolved.requireValue();
        Iterable<Alias> bbcSIdAliases = Iterables.filter(channel.getAliases(), IS_BBC_SID_ALIAS);
        if (Iterables.isEmpty(bbcSIdAliases)) {
            throw new NoSuchChannelAliasException(BBC_SID_NAMESPACE);
        }
        Alias sidAlias = Iterables.getOnlyElement(bbcSIdAliases);
        return sidAlias.getValue();
    }

    @Override
    public String resolveSId(Content content) {
        return resolveServiceId(content.getPresentationChannel());
    }
}
