package org.atlasapi.feeds.interlinking.delta;

import static com.metabroadcast.common.time.DateTimeZones.UTC;
import static org.joda.time.DateTimeConstants.AUGUST;
import static org.junit.Assert.*;
import nu.xom.Document;

import org.atlasapi.feeds.interlinking.C4PlaylistToInterlinkFeedAdapter;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeed;
import org.atlasapi.feeds.interlinking.outputting.InterlinkFeedOutputter;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.Iterators;
import com.metabroadcast.common.base.Maybe;

public class InterlinkingDeltaUpdaterTest {

    private final Mockery context = new Mockery();
    private final LastUpdatedContentFinder contentFinder = context.mock(LastUpdatedContentFinder.class);
    private final InterlinkFeedOutputter outputter = new InterlinkFeedOutputter();
    private final PlaylistToInterlinkFeed adapter = new C4PlaylistToInterlinkFeedAdapter();
    
    private final InterlinkingDeltaUpdater updater = new InterlinkingDeltaUpdater(contentFinder, outputter, adapter);

    
    @Test
    public void testUpdatingNewFeed() {
        
        InterlinkingDelta delta = InterlinkingDelta.deltaFor(Maybe.<Document>nothing(), null);
        
        DateTime now = new DateTime(2011,AUGUST,26, 18,43,34,123, UTC);
        final DateTime startOfDay = now.withTime(0, 0, 0, 0);
        DateTime endOfDay = startOfDay.plusDays(1);
        
        final Item item1 = new Item("http://www.channel4.com/programmes/wildfire/episode-guide/series-1/episode-1", "c4:wildfire-20663895", Publisher.C4_PMLSD);
        item1.setTitle("Wildfire Series 1 Episode 1");
        item1.setLastUpdated(now.minusHours(5));
        item1.setThisOrChildLastUpdated(item1.getLastUpdated());

        final Item item2 = new Item("http://www.channel4.com/programmes/wildfire/episode-guide/series-1/episode-2", "c4:wildfire-20663444", Publisher.C4_PMLSD);
        item2.setTitle("Wildfire Series 1 Episode 2");
        item2.setLastUpdated(now.minusHours(1));
        item2.setThisOrChildLastUpdated(item2.getLastUpdated());
        
        context.checking(new Expectations(){{
            oneOf(contentFinder).updatedSince(Publisher.C4_PMLSD, startOfDay);
                will(returnValue(Iterators.forArray(item1)));
            oneOf(contentFinder).updatedSince(Publisher.C4_PMLSD, item1.getLastUpdated());
                will(returnValue(Iterators.forArray(item2)));
        }});
        
        InterlinkingDelta updatedFeed = updater.updateFeed(delta, startOfDay, endOfDay);
        
        assertEquals(item1.getLastUpdated(), updatedFeed.lastUpdated());
        
        InterlinkingDelta updatedFeed2 = updater.updateFeed(updatedFeed, endOfDay);
        
        assertEquals(item2.getLastUpdated(), updatedFeed2.lastUpdated());
        
        
    }

}
