package org.atlasapi.feeds.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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


public class DescriptionWatermarkerTest {

    private static final String SHORT_DESCRIPTION = "foo";
    private static final DateTime NO_WATERMARK_TIME = new DateTime(2014, DateTimeConstants.FEBRUARY, 10, 11, 30, 0, 0).withZone(DateTimeZone.UTC);
    private static final DateTime WATERMARK_TIME = new DateTime(2014, DateTimeConstants.FEBRUARY, 10, 10, 30, 0, 0).withZone(DateTimeZone.UTC);
    private static final String DESCRIPTION = "This is a description.";
    private static final String WATERMARKED_DESCRIPTION = "This is a  description.";

    private static final String WATERMARK_CHANNEL = "http://example.com/a";
    private static final String NOT_WATERMARK_CHANNEL = "http://example.com/b";

    private static final DescriptionWatermarker watermarker = new DescriptionWatermarker(ImmutableSet.of(WATERMARK_CHANNEL));
    
    @Test
    public void testAddsWatermark() {
        Item item = testItem(WATERMARK_CHANNEL, WATERMARK_TIME);
        assertThat(watermarker.watermark(broadcast(item), item.getDescription()), is(WATERMARKED_DESCRIPTION));
    }
    
    @Test
    public void testDoesntAddWatermarkOnOddHourOfDay() {
        Item item = testItem(WATERMARK_CHANNEL, NO_WATERMARK_TIME);
        assertThat(watermarker.watermark(broadcast(item), item.getDescription()), is(DESCRIPTION));
    }
    
    @Test
    public void testDoesntAddWatermarkOnDifferentChannel() {
        Item item = testItem(NOT_WATERMARK_CHANNEL, WATERMARK_TIME);
        assertThat(watermarker.watermark(broadcast(item), item.getDescription()), is(DESCRIPTION));
    }
    
    @Test
    public void testDoesntAddWatermarkOnDifferentChannelOnOddHour() {
        Item item = testItem(NOT_WATERMARK_CHANNEL, NO_WATERMARK_TIME);
        assertThat(watermarker.watermark(broadcast(item), item.getDescription()), is(DESCRIPTION));
    }
    
    @Test
    public void testNoopOnDescriptionNotWatermarkable() {
        Item item = testItem(WATERMARK_CHANNEL, WATERMARK_TIME);
        item.setDescription(SHORT_DESCRIPTION);
        assertThat(watermarker.watermark(broadcast(item), item.getDescription()), is(SHORT_DESCRIPTION));
    }
    
    @Test
    public void testNullable() {
        Broadcast broadcast = ComplexBroadcastTestDataBuilder.broadcast()
                .withChannel(WATERMARK_CHANNEL)
                .withStartTime(WATERMARK_TIME)
                .build();
        
        assertThat(watermarker.watermark(null, null), is(nullValue()));
        assertThat(watermarker.watermark(null, DESCRIPTION), is(DESCRIPTION));
        assertThat(watermarker.watermark(broadcast, null), is(nullValue()));
        
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
