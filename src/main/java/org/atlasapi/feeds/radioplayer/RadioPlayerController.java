package org.atlasapi.feeds.radioplayer;

import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerOdUriResolver;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

@Controller
public class RadioPlayerController {
    
    private static final Joiner JOIN_ON_COMMA = Joiner.on(',');
    
    private final Map<Publisher, RadioPlayerOdUriResolver> odUriResolver;
    private final Set<Publisher> publishers;

    public RadioPlayerController(final LastUpdatedContentFinder lastUpdatedContentFinder, final ContentLister contentLister, Set<Publisher> publishers) {
        this.publishers = publishers;
        this.odUriResolver = Maps.asMap(publishers, new Function<Publisher, RadioPlayerOdUriResolver> () {
            @Override
            public RadioPlayerOdUriResolver apply(Publisher input) {
                return new RadioPlayerOdUriResolver(contentLister, lastUpdatedContentFinder, input);
            }
        });
    }
    
    @RequestMapping("feeds/{publisher}/ukradioplayer/{filename}.xml")
    public void xmlForFilename(@PathVariable("publisher") String publisherStr, @PathVariable("filename") String filename, 
            HttpServletResponse response) throws IOException {

        RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(filename);

        if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {

            FileType feedType = matcher.type().requireValue();
            try {
                Publisher publisher = Publisher.valueOf(publisherStr.trim().toUpperCase());
                RadioPlayerOdUriResolver publisherOdUriResolver = odUriResolver.get(publisher);
                if (publisherOdUriResolver == null) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Publisher not allowed. Must be one of " + JOIN_ON_COMMA.join(publishers));
                }
                 
                RadioPlayerFeedSpec spec;
                if (matcher.type().requireValue().equals(PI)) {
                    spec = new RadioPlayerPiFeedSpec(matcher.service().requireValue(), matcher.date().requireValue());
                } else if (matcher.type().requireValue().equals(OD)) {
                    DateTime since = matcher.date().requireValue().toDateTimeAtStartOfDay().minusHours(2);
                    spec = new RadioPlayerOdFeedSpec(matcher.service().requireValue(), matcher.date().requireValue(), Optional.of(since), publisherOdUriResolver.getServiceToUrisMapSince(since).get(matcher.service().requireValue()));
                } else {
                    throw new IllegalArgumentException("Unknown file type");
                }
                
                RadioPlayerFeedCompiler.valueOf(publisher, feedType).compileFeedFor(spec, response.getOutputStream());
            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid publisher" + e.getMessage());
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }

        } else {
            if (matcher.service().isNothing()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unkown Service");
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognised filename pattern");
            }

        }
    }

}
