package org.atlasapi.feeds.utils;

import java.util.Set;

import javax.annotation.Nullable;

import org.atlasapi.media.entity.Broadcast;

import com.google.common.collect.ImmutableSet;


public class DescriptionWatermarker {

    private final Set<String> channelsToWatermark;

    public DescriptionWatermarker(Iterable<String> channelsToWatermark) {
        this.channelsToWatermark = ImmutableSet.copyOf(channelsToWatermark);
    }
    
    public String watermark(@Nullable Broadcast broadcast, @Nullable String description) {
        if (description == null || broadcast == null) {
            return description;
        }
        if (channelsToWatermark.contains(broadcast.getBroadcastOn())
                && broadcast.getTransmissionTime().getHourOfDay() % 2 == 0) {
            return doWatermark(description);
        } else {
            return description;
        }
    }
    
    private String doWatermark(String description) {
        int lastIndex = description.lastIndexOf(" ");
        if (lastIndex == -1) {
            return description;
        }
        return new StringBuilder(description).replace(lastIndex, lastIndex + 1, "  ").toString();
    }
}
