package org.atlasapi.feeds.radioplayer.outputting;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.joda.time.DateTime;

public class NoItemsException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final DateTime date;
    private final RadioPlayerService service;

    public NoItemsException(DateTime date, RadioPlayerService service) {
        this.date = date;
        this.service = service;
    }

    @Override
    public String getMessage() {
        return String.format("No items to create feed for %s for %s", service.getName(), date.toString("dd/MM/yyyy"));
    }

}
