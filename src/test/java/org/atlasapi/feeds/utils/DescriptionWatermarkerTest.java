package org.atlasapi.feeds.xmltv;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.testing.ComplexBroadcastTestDataBuilder;
import org.atlasapi.media.entity.testing.ComplexItemTestDataBuilder;
import org.atlasapi.media.entity.testing.VersionTestDataBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;


public class XmlTvFeedDescriptionWatermarkerTest {

    private static final String DESCRIPTION = "This is a description.";
    private static final String WATERMARKED_DESCRIPTION = "This is a  description.";

    private static final String WATERMARK_CHANNEL = "http://www.bbc.co.uk/services/bbctwo";
    private static final String NOT_WATERMARK_CHANNEL = "http://www.bbc.co.uk/services/bbcone";

    private static final XmlTvFeedDescriptionWatermarker watermarker = new XmlTvFeedDescriptionWatermarker(ImmutableSet.of(WATERMARK_CHANNEL));
    
    @Test
    public void testAddsWatermark() {
        Item item = testItem(WATERMARK_CHANNEL, new DateTime(2014, DateTimeConstants.FEBRUARY, 10, 10, 30, 0, 0).withZone(DateTimeZone.UTC));
        assertThat(watermarker.watermark(broadcast(item), item.getDescription()), is(WATERMARKED_DESCRIPTION));
    }
    
    @Test
    public void testDoesntAddWatermarkOnDifferentChannel() {
        Item item = testItem(NOT_WATERMARK_CHANNEL, new DateTime(2014, DateTimeConstants.FEBRUARY, 10, 10, 30, 0, 0).withZone(DateTimeZone.UTC));
        assertThat(watermarker.watermark(broadcast(item), item.getDescription()), is(DESCRIPTION));
    }
    
    @Test
    public void testDoesntAddWatermarkOnOddHourOfDay() {
        Item item = testItem(NOT_WATERMARK_CHANNEL, new DateTime(2014, DateTimeConstants.FEBRUARY, 10, 11, 30, 0, 0).withZone(DateTimeZone.UTC));
        assertThat(watermarker.watermark(broadcast(item), item.getDescription()), is(DESCRIPTION));
    }
    
    private Broadcast broadcast(Item item) {
        return Iterables.getOnlyElement(Iterables.getOnlyElement(item.getVersions()).getBroadcasts());
    }
    private Item testItem(String channel, DateTime startTime) {
        Broadcast broadcast = ComplexBroadcastTestDataBuilder.broadcast()
                .withChannel(channel)
                .withStartTime(startTime)
                .build();

        return ComplexItemTestDataBuilder.complexItem()
                        .withVersions(VersionTestDataBuilder.version().withBroadcasts(broadcast).build())
                        .withDescription(DESCRIPTION)
                        .build();
    }
}
