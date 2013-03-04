package org.atlasapi.feeds.tvanytime;

import java.io.OutputStream;

import org.atlasapi.media.entity.Item;

public interface TvAnytimeGenerator {

    void generateXml(Iterable<Item> items, OutputStream outStream, boolean includeServiceInformation);
}
