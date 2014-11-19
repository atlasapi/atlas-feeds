package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.assertEquals;

import org.atlasapi.feeds.youview.NoChannelFoundException;
import org.atlasapi.feeds.youview.NoSuchChannelAliasException;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.junit.Test;
import org.mockito.Mockito;

import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class ChannelResolvingBbcServiceIdResolverTest {

    private static final String SERVICE_ID = "serviceId";
    private static final String SID_NAMESPACE = "bbc:service:id";
    private Clock clock = new TimeMachine();
    private ChannelResolver channelResolver = Mockito.mock(ChannelResolver.class);
    private final BbcServiceIdResolver serviceIdResolver = new ChannelResolvingBbcServiceIdResolver(channelResolver); 
    
    @SuppressWarnings("deprecation") // Maybe
    @Test(expected = NoChannelFoundException.class)
    public void testExceptionThrownWhenNoMatchingChannelFound() {
        String broadcastOn = "bbcOne";
        
        Mockito.when(channelResolver.fromUri(broadcastOn)).thenReturn(Maybe.<Channel>nothing());
        
        serviceIdResolver.resolveSId(createBroadcastOn(broadcastOn));
    }

    @SuppressWarnings("deprecation") // Maybe
    @Test(expected = NoSuchChannelAliasException.class)
    public void testExceptionThrownWhenChannelHasNoMatchingAlias() {
        String broadcastOn = "bbcOne";
        Channel channel = new Channel(Publisher.METABROADCAST, "BBC One", "bbc_one", true, MediaType.VIDEO, broadcastOn);
        
        Mockito.when(channelResolver.fromUri(broadcastOn)).thenReturn(Maybe.just(channel));
        
        serviceIdResolver.resolveSId(createBroadcastOn(broadcastOn));
    }

    @SuppressWarnings("deprecation") // Maybe
    @Test
    public void testResolvesCorrectAliasWhenChannelHasAMatchingAlias() {
        String broadcastOn = "bbcOne";
        Channel channel = new Channel(Publisher.METABROADCAST, "BBC One", "bbc_one", true, MediaType.VIDEO, broadcastOn);
        Alias alias = new Alias(SID_NAMESPACE, SERVICE_ID);
        channel.addAlias(alias);
        
        Mockito.when(channelResolver.fromUri(broadcastOn)).thenReturn(Maybe.just(channel));
        
        String resolved = serviceIdResolver.resolveSId(createBroadcastOn(broadcastOn));
        
        assertEquals(SERVICE_ID, resolved);
    }
    
    @SuppressWarnings("deprecation") // Maybe
    @Test
    public void testResolvesCorrectAliasFromContentPresentationChannelWhenChannelHasAMatchingAlias() {
        String broadcastOn = "bbcOne";
        Channel channel = new Channel(Publisher.METABROADCAST, "BBC One", "bbc_one", true, MediaType.VIDEO, broadcastOn);
        Alias alias = new Alias(SID_NAMESPACE, SERVICE_ID);
        channel.addAlias(alias);
        
        Mockito.when(channelResolver.fromUri(broadcastOn)).thenReturn(Maybe.just(channel));
        
        String resolved = serviceIdResolver.resolveSId(createContentOn(broadcastOn));
        
        assertEquals(SERVICE_ID, resolved);
    }

    private Content createContentOn(String broadcastOn) {
        Film film = new Film();
        film.setPresentationChannel(broadcastOn);
        return film;
    }

    private Broadcast createBroadcastOn(String broadcastOn) {
        return new Broadcast(broadcastOn, clock.now(), clock.now().plusMinutes(30));
    }

}
