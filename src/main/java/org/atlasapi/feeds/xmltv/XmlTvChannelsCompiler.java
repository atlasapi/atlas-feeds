package org.atlasapi.feeds.xmltv;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.xmltv.XmlTvChannelLookup.XmlTvChannel;

import com.google.common.base.Charsets;

public class XmlTvChannelsCompiler {

    private final Map<Integer, XmlTvChannel> channels;

    public XmlTvChannelsCompiler(Map<Integer, XmlTvChannel> channels) {
        this.channels = channels;
    }

    public void compileChannelsFeed(OutputStream stream) throws IOException {
        Writer writer = new OutputStreamWriter(stream, Charsets.UTF_8);
        writer.write(XmlTvModule.FEED_PREABMLE);
        writer.write('\n');
        writer.flush();
    }
    
}
