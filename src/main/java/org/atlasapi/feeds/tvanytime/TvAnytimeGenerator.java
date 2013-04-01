package org.atlasapi.feeds.tvanytime;

import java.io.OutputStream;

import org.atlasapi.media.content.Content;

public interface TvAnytimeGenerator {

    void generateXml(Iterable<Content> contents, OutputStream outStream, boolean includeServiceInformation);
}
