package org.atlasapi.feeds.interlinking.delta;

import org.joda.time.DateTime;

public interface InterlinkingDeltaStore {

    void storeDelta(DateTime time, InterlinkingDelta file);

    InterlinkingDelta getExistingDelta(DateTime day);

}