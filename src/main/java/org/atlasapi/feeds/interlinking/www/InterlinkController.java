package org.atlasapi.feeds.interlinking.www;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.ContentQueryBuilder;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.feeds.interlinking.DelegatingPlaylistToInterlinkAdapter;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeed;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeedAdapter;
import org.atlasapi.feeds.interlinking.outputting.InterlinkFeedOutputter;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
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
    
    private final static String FEED_ID = "http://interlinking.channel4.com/feeds/bbc-interlinking/";
    private final ContentResolver resolver;
    private final InterlinkFeedOutputter outputter = new InterlinkFeedOutputter();
    private final PlaylistToInterlinkFeed adapter;
    private final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd").withZone(DateTimeZones.LONDON);
    private final KnownTypeQueryExecutor executor;

    public InterlinkController(ContentResolver resolver, KnownTypeQueryExecutor executor, Map<Publisher, PlaylistToInterlinkFeed> delegates) {
        this.resolver = resolver;
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
        ContentQuery query = ContentQueryBuilder.query()
                .equalTo(Attributes.DESCRIPTION_PUBLISHER, Publisher.C4)
                .after(Attributes.BRAND_THIS_OR_CHILD_LAST_UPDATED, from)
                .build();
        
//        List<Content> brands = executor.discover(query.copyWithApplicationConfiguration(query.getConfiguration().copyWithIncludedPublishers(ImmutableList.of(Publisher.C4))));
        
//        outputter.output(adapter.fromBrands(FEED_ID+date, Publisher.C4, from, to, brands), response.getOutputStream(), false);
        
        throw new UnsupportedOperationException("Can no longer discover");
    }
    
    @RequestMapping("/feeds/bbc-interlinking/bootstrap")
    public void bootstrapFeed(HttpServletResponse response) throws IOException {
    	response.setContentType(MimeType.APPLICATION_ATOM_XML.toString());
    	response.setStatus(HttpServletResponse.SC_OK);

    	DateTime from = new DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZones.LONDON);
    	DateTime to = new DateTime(DateTimeZones.LONDON);
    	
    	ContentQuery query = ContentQueryBuilder.query()
    		.equalTo(Attributes.DESCRIPTION_PUBLISHER, Publisher.C4)
    	.build();
//    	List<Content> brands = executor.discover(query.copyWithApplicationConfiguration(query.getConfiguration().copyWithIncludedPublishers(ImmutableList.of(Publisher.C4))));
//    	outputter.output(adapter.fromBrands(FEED_ID+"bootstrap", Publisher.C4, from, to, brands), response.getOutputStream(), true);
    	throw new UnsupportedOperationException("Can no longer discover");
    }
}
