package org.atlasapi.feeds.interlinking.www;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.interlinking.DelegatingPlaylistToInterlinkAdapter;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeed;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeedAdapter;
import org.atlasapi.feeds.interlinking.outputting.InterlinkFeedOutputter;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.time.DateTimeZones;

@Controller
public class InterlinkController {
    
    public final static String FEED_ID = "http://interlinking.channel4.com/feeds/bbc-interlinking/";
    private final InterlinkFeedOutputter outputter = new InterlinkFeedOutputter();
    private final PlaylistToInterlinkFeed adapter;
    private final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd").withZone(DateTimeZones.LONDON);
    private final LastUpdatedContentFinder executor;

    public InterlinkController(LastUpdatedContentFinder executor, Map<Publisher, PlaylistToInterlinkFeed> delegates) {
        this.executor = executor;
        this.adapter = new DelegatingPlaylistToInterlinkAdapter(delegates, new PlaylistToInterlinkFeedAdapter());
    }

    @RequestMapping("/feeds/bbc-interlinking/{date}")
    public void updatedFeed(HttpServletResponse response, @PathVariable String date) throws IOException {
        DateTime from = fmt.parseDateTime(date);
        DateTime to = from.plusDays(1);
        response.setContentType(MimeType.APPLICATION_ATOM_XML.toString());
        response.setStatus(HttpServletResponse.SC_OK);

        // NB We don't include the 'to' datetime in the query to
        // avoid newer brand updates masking older item updates 
        Iterator<Content> content = executor.updatedSince(Publisher.C4, from);
		outputter.output(adapter.fromContent(FEED_ID+date, Publisher.C4, from, to, content), response.getOutputStream(), false, from);
    }
    
    @RequestMapping("/feeds/bbc-interlinking/bootstrap")
    public void bootstrapFeed(HttpServletResponse response) throws IOException {
    	response.setContentType(MimeType.APPLICATION_ATOM_XML.toString());
    	response.setStatus(HttpServletResponse.SC_OK);
    	
    	DateTime from = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.LONDON);
    	DateTime to = new DateTime(DateTimeZones.LONDON);

    	Iterator<Content> content = executor.updatedSince(Publisher.C4, from);
    	outputter.output(adapter.fromContent(FEED_ID + "bootstrap", Publisher.C4, from, to, content), response.getOutputStream(), true, from);
    }
}
