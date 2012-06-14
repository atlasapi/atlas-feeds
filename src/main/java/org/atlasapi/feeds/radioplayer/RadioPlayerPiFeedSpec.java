package org.atlasapi.feeds.radioplayer;

import org.joda.time.LocalDate;

import com.google.common.base.Objects;

public class RadioPlayerPiFeedSpec extends RadioPlayerFeedSpec {
    
    public RadioPlayerPiFeedSpec(RadioPlayerService service, LocalDate day) {
        super(service, day);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(RadioPlayerPiFeedSpec.class).add("service", service).add("day", day.toString("dd/MM/yyyy")).toString();
    }

    @Override
    protected String filenameSuffix() {
        return "PI";
    }
}
