package org.atlasapi.feeds.tvanytime;

import java.io.File;

import org.atlasapi.media.entity.Item;

public interface TvAnytimeGenerator {

    void generateXml(Iterable<Item> items, File file, boolean isBootstrap);
}
