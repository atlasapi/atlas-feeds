package org.atlasapi.feeds.interlinking;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.testing.BrandTestDataBuilder;
import org.atlasapi.media.entity.testing.ComplexItemTestDataBuilder;
import org.atlasapi.persistence.content.ContentResolver;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.metabroadcast.common.time.DateTimeZones;

@RunWith( MockitoJUnitRunner.class )
public class C4PlaylistToInterlinkFeedAdapterTest {

    private static final String BRAND_URI = "http://pmlsc.channel4.com/pmlsd/hollyoaks";
    private final ContentResolver contentResolver = mock(ContentResolver.class);
    private final C4PlaylistToInterlinkFeedAdapter adapter = new C4PlaylistToInterlinkFeedAdapter(contentResolver);
    
    @Test
    public void testBroadcastId() {
        
        
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
    
    @Test
    public void testLink() {
        Item item = ComplexItemTestDataBuilder
                           .complexItem()
                           .withAliasUrls("http://pmlsc.channel4.com/pmlsd/hollyoaks/episode-guide/series-23/episode-214")
                           .build(); 
        
        assertThat(adapter.linkFrom(item), is("http://www.channel4.com/programmes/hollyoaks/episode-guide/series-23/episode-214"));
    }

    @Test
    public void testId() {
        Item item = ComplexItemTestDataBuilder
                .complexItem()
                .withAliasUrls("http://pmlsc.channel4.com/pmlsd/hollyoaks/episode-guide/series-23/episode-214")
                .build();

        assertThat(adapter.idFrom(item), is("tag:www.channel4.com,2009:/programmes/hollyoaks/episode-guide/series-23/episode-214"));

    }

    @Test
    public void testIdWithoutAliasUrl() {
        Item item = ComplexItemTestDataBuilder
                .complexItem()
                .withUri("http://pmlsc.channel4.com/pmlsd/58191/001")
                .build();

        assertThat(adapter.idFrom(item), is("tag:www.channel4.com,2009:/programmes/58191/001"));

    }
    @Test
    public void testLinkNoAliasUri() {
        Brand brand = BrandTestDataBuilder
                           .brand()
                           .withCanonicalUri(BRAND_URI)
                           .build();

        Episode episode =  new Episode();
        Episode.copyTo(ComplexItemTestDataBuilder.complexItem().build(), episode);
        episode.setContainer(brand);
        
        assertThat(adapter.linkFrom(episode), is("http://www.channel4.com/programmes/hollyoaks"));
    }

}
