package org.atlasapi.feeds.xmltv;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map.Entry;

import org.atlasapi.media.entity.Channel;

import com.google.common.base.Charsets;

public class XmlTvChannelsCompiler {

    private final XmlTvChannelLookup channels;

    public XmlTvChannelsCompiler(XmlTvChannelLookup channels) {
        this.channels = channels;
    }

    public void compileChannelsFeed(OutputStream stream) throws IOException {
        Writer writer = new OutputStreamWriter(stream, Charsets.UTF_8);
        writer.write(XmlTvModule.FEED_PREABMLE);
        for (Entry<Integer, Channel> channelMapping : channels.entrySet()) {
            writer.write(String.format("\n%s|%s", channelMapping.getKey(), channelMapping.getValue().title()));
        }
        writer.flush();
    }
    
}
