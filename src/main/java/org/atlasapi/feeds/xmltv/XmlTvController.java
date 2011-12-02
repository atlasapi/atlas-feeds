package org.atlasapi.feeds.xmltv;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.entity.Channel;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.media.MimeType;

@Controller
public class XmlTvController {

    private final DateTimeFormatter dateFormat = ISODateTimeFormat.basicDate();
    private final XmlTvFeedCompiler compiler;
    private final XmlTvChannelLookup channelLookup;

    public XmlTvController(XmlTvFeedCompiler compiler, XmlTvChannelLookup channelLookup) {
        this.compiler = compiler;
        this.channelLookup = channelLookup;
    }
    
    @RequestMapping("/feeds/xmltv/{id}.dat")
    public void getFeed(HttpServletResponse response, @PathVariable Integer id, @RequestParam(value="from",required=false) String startDay) throws IOException {
        
        Channel channel = channelLookup.get(id);
        
        if (channel == null) {
            response.sendError(HttpStatusCode.NOT_FOUND.code(), String.format("Channel %s not found", id));
        }
        
        LocalDate startDate;
        if(Strings.isNullOrEmpty(startDay)) {
            startDate = new LocalDate();
        } else {
            if(startDay.matches("\\d{8}")) {
                startDate = dateFormat.parseDateTime(startDay).toLocalDate();
            } else {
                response.sendError(HttpStatusCode.BAD_REQUEST.code(), String.format("Invalid start date: %s",startDay));
                return;
            }
        }

        response.setContentType(MimeType.TEXT_PLAIN.toString());
        response.setCharacterEncoding(Charsets.UTF_8.displayName());
        compiler.compileFeed(daysFrom(startDate), channel, response.getOutputStream());
        
    }

    private Range<LocalDate> daysFrom(LocalDate startDay) {
        return Ranges.closed(startDay, startDay.plusWeeks(2));
    }
    
    @RequestMapping("/feeds/xmltv/channels.dat")
    public void getChannels(HttpServletResponse response) throws IOException {
        response.setContentType(MimeType.TEXT_PLAIN.toString());
        response.setCharacterEncoding(Charsets.UTF_8.displayName());
        response.getWriter().println(XmlTvModule.FEED_PREABMLE);
        for (Entry<Integer, Channel> channelMapping : channelLookup.entrySet()) {
            response.getWriter().println(String.format("%s|%s", channelMapping.getKey(), channelMapping.getValue().title()));
        }
    }
}
