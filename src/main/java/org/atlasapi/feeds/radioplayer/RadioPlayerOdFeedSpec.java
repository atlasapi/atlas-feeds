package org.atlasapi.feeds.radioplayer;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class RadioPlayerOdFeedSpec extends RadioPlayerFeedSpec {
    
    private final Optional<DateTime> since;
    private final Iterable<String> uris;

    public RadioPlayerOdFeedSpec(RadioPlayerService service, LocalDate day, Optional<DateTime> since, Iterable<String> uris) {
        super(service, day);
        this.since = checkNotNull(since);
        this.uris = checkNotNull(uris);
    }

    public Optional<DateTime> getSince() {
        return since;
    }

    public Iterable<String> getUris() {
        return uris;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(RadioPlayerOdFeedSpec.class).add("service", service).add("day", day.toString("dd/MM/yyyy")).add("since", since).toString();
    }

    @Override
    protected String filenameSuffix() {
        return "OD";
    }

}
