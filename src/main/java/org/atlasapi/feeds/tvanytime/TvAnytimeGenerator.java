package org.atlasapi.feeds.tvanytime;

import java.io.OutputStream;

import org.atlasapi.media.entity.Content;

public interface TvAnytimeGenerator {

    void generateXml(Iterable<Content> contents, OutputStream outStream);
}
