package org.atlasapi.feeds.interlinking.www;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.interlinking.DelegatingPlaylistToInterlinkAdapter;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeed;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeedAdapter;
import org.atlasapi.feeds.interlinking.outputting.InterlinkFeedOutputter;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentTable;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.listing.ContentListingCriteria;
import org.atlasapi.persistence.content.listing.ContentListingHandler;
import org.atlasapi.persistence.content.listing.ContentListingProgress;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.time.DateTimeZones;

@Controller
public class InterlinkController {
    
    private final static String FEED_ID = "http://interlinking.channel4.com/feeds/bbc-interlinking/";
    private final InterlinkFeedOutputter outputter = new InterlinkFeedOutputter();
    private final PlaylistToInterlinkFeed adapter;
    private final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd").withZone(DateTimeZones.LONDON);
    private final ContentLister executor;

    public InterlinkController(ContentLister executor, Map<Publisher, PlaylistToInterlinkFeed> delegates) {
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
        final List<Content> content = loadIntoList(new ContentListingCriteria().forPublisher(Publisher.C4).updatedSince(from));
        outputter.output(adapter.fromContent(FEED_ID+date, Publisher.C4, from, to, content), response.getOutputStream(), false);
    }
    
    @RequestMapping("/feeds/bbc-interlinking/bootstrap")
    public void bootstrapFeed(HttpServletResponse response) throws IOException {
    	response.setContentType(MimeType.APPLICATION_ATOM_XML.toString());
    	response.setStatus(HttpServletResponse.SC_OK);

    	final List<Content> content = loadIntoList(new ContentListingCriteria().forPublisher(Publisher.C4));
    	
    	DateTime from = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.LONDON);
    	DateTime to = new DateTime(DateTimeZones.LONDON);
    	outputter.output(adapter.fromContent(FEED_ID + "bootstrap", Publisher.C4, from, to, content), response.getOutputStream(), true);
    }

    private List<Content> loadIntoList(ContentListingCriteria criteria) {
        final List<Content> brands = Lists.newArrayList();
    	executor.listContent(ImmutableSet.of(ContentTable.TOP_LEVEL_CONTAINERS, ContentTable.PROGRAMME_GROUPS, ContentTable.CHILD_ITEMS), criteria, new ContentListingHandler() {
            @Override
            public boolean handle(Content content, ContentListingProgress progress) {
                brands.add(content);
                return true;
            }
        });
        return brands;
    }
}
