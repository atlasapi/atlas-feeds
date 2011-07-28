package org.atlasapi.feeds.lakeview;

import java.io.IOException;
import java.io.OutputStream;

import nu.xom.Document;

public interface XmlFeedOutputter {

    void outputTo(Document document, OutputStream out) throws IOException;

}