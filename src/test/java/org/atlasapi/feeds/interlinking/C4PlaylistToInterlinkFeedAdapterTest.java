package org.atlasapi.feeds.interlinking;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.atlasapi.media.entity.Broadcast;
import org.joda.time.DateTime;
import org.junit.Test;

import com.metabroadcast.common.time.DateTimeZones;

public class C4PlaylistToInterlinkFeedAdapterTest {

    @Test
    public void testBroadcastId() {
        
        C4PlaylistToInterlinkFeedAdapter adapter = new C4PlaylistToInterlinkFeedAdapter();
        
        DateTime start = new DateTime(DateTimeZones.UTC);
        DateTime end = new DateTime(DateTimeZones.UTC);
        
        Broadcast b1 = new Broadcast("http://www.e4.com", start, end).withId("Not used");
        b1.addAliasUrl("tag:www.e4.com,2009:slot/10401");
        
        assertThat(adapter.broadcastId(b1), is("tag:www.e4.com,2009:slot/E410401"));

        Broadcast b2 = new Broadcast("http://www.e4.com", start, end).withId("Not used");
        b2.addAliasUrl("tag:www.e4.com,2009:slot/E410401");
        
        assertThat(adapter.broadcastId(b2), is("tag:www.e4.com,2009:slot/E410401"));
        
        Broadcast b3 = new Broadcast("http://www.channel4.com/4seven", start, end)
            .withId("4s:27945505");
        b3.addAliasUrl("tag:www.channel4.com,2009:slot/4S27945505");
        
        assertThat(adapter.broadcastId(b3), is("tag:www.channel4.com,2009:slot/4S27945505"));
        
    }

}
