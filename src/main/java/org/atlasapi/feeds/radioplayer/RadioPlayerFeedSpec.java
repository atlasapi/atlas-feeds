package org.atlasapi.feeds.radioplayer;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.LocalDate;

public abstract class RadioPlayerFeedSpec {
    
    protected final RadioPlayerService service;
    protected final LocalDate day;
    
    public RadioPlayerFeedSpec(RadioPlayerService service, LocalDate day) {
        this.service = checkNotNull(service);
        this.day = checkNotNull(day);
    }

    public RadioPlayerService getService() {
        return service;
    }

    public LocalDate getDay() {
        return day;
    }
    
    public String filename() {
        
        return String.format("%s_%s_%s.xml", day.toString("yyyyMMdd"), service.getRadioplayerId(), filenameSuffix());
    }
    
    protected abstract String filenameSuffix();
}
