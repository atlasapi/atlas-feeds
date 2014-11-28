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

    private static final String BBC_MASTERBRAND_ID_NAMESPACE = "bbc:masterbrand:id";
    
    private final ChannelResolver channelResolver;

    public ChannelResolvingBbcServiceIdResolver(ChannelResolver channelResolver) {
        this.channelResolver = checkNotNull(channelResolver);
    }

    @Override
    public String resolveSId(Broadcast broadcast) {
        return resolveServiceId(broadcast.getBroadcastOn(), BBC_SID_NAMESPACE);
    }

    private String resolveServiceId(String channelUri, String namespace) {
        Maybe<Channel> resolved = channelResolver.fromUri(channelUri);
        if (resolved.isNothing()) {
            throw new NoChannelFoundException(channelUri);
        }
        Channel channel = resolved.requireValue();
        Iterable<Alias> bbcSIdAliases = Iterables.filter(channel.getAliases(), hasNamespace(namespace));
        if (Iterables.isEmpty(bbcSIdAliases)) {
            throw new NoSuchChannelAliasException(namespace);
        }
        Alias sidAlias = Iterables.getOnlyElement(bbcSIdAliases);
        return sidAlias.getValue();
    }

    @Override
    public String resolveSId(Content content) {
        return resolveServiceId(content.getPresentationChannel(), BBC_SID_NAMESPACE);
    }

    @Override
    public String resolveMasterBrandId(Content content) {
        return resolveServiceId(content.getPresentationChannel(), BBC_MASTERBRAND_ID_NAMESPACE);
    }
    
    private Predicate<Alias> hasNamespace(final String namespace) {
        return new Predicate<Alias>() {
            @Override
            public boolean apply(Alias input) {
                return namespace.equals(input.getNamespace());
            }
        };
    }
}
