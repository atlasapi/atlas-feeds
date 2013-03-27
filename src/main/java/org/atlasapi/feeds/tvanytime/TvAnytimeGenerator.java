package org.atlasapi.feeds.tvanytime;

import java.io.OutputStream;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;

public interface TvAnytimeGenerator {

    void generateXml(Iterable<Content> contents, OutputStream outStream, boolean includeServiceInformation);
}
