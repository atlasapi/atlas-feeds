package org.atlasapi.feeds.lakeview;

import java.io.IOException;
import java.io.OutputStream;

import nu.xom.Document;
import nu.xom.Serializer;

import com.google.common.base.Charsets;

public class SerializingFeedOutputter implements XmlFeedOutputter {
    
    @Override
    public void outputTo(Document document, OutputStream out) throws IOException {
        output(document, out);
    }

    private void output(Document document, OutputStream out) throws IOException {
        Serializer serializer = new Serializer(out, Charsets.UTF_8.toString());
        serializer.setIndent(4);
        serializer.setLineSeparator("\n");
        serializer.write(document);
    }

}
