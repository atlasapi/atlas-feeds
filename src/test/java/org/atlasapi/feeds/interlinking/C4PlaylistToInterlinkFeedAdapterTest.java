package org.atlasapi.feeds.interlinking;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Channel;
import org.joda.time.DateTime;
import org.junit.Test;

import com.metabroadcast.common.time.DateTimeZones;

public class C4PlaylistToInterlinkFeedAdapterTest {

    @Test
    public void testBroadcastId() {
        
        C4PlaylistToInterlinkFeedAdapter adapter = new C4PlaylistToInterlinkFeedAdapter(null);
        
        Broadcast b1 = new Broadcast(Channel.E_FOUR.uri(), new DateTime(DateTimeZones.UTC), new DateTime(DateTimeZones.UTC)).withId("Not used");
        b1.addAlias("tag:www.e4.com,2009:slot/10401");
        
        assertThat(adapter.broadcastId(b1), is("tag:www.e4.com,2009:slot/E410401"));

        Broadcast b2 = new Broadcast(Channel.E_FOUR.uri(), new DateTime(DateTimeZones.UTC), new DateTime(DateTimeZones.UTC)).withId("Not used");
        b2.addAlias("tag:www.e4.com,2009:slot/E410401");
        
        assertThat(adapter.broadcastId(b2), is("tag:www.e4.com,2009:slot/E410401"));
        
    }

}
