package org.atlasapi.feeds.radioplayer;

import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

import java.io.IOException;

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

import com.google.common.base.Optional;

@Controller
public class RadioPlayerController {
    
    private final RadioPlayerOdUriResolver odUriResolver;
    private final Publisher publisher;

    public RadioPlayerController(LastUpdatedContentFinder lastUpdatedContentFinder, ContentLister contentLister, Publisher publisher) {
        this.publisher = publisher;
        odUriResolver = new RadioPlayerOdUriResolver(contentLister, lastUpdatedContentFinder, publisher);
    }

    @RequestMapping("feeds/ukradioplayer/{filename}.xml")
    public void xmlForFilename(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {

        RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(filename);

        if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {

            FileType feedType = matcher.type().requireValue();
            try {
                RadioPlayerFeedSpec spec;
                if (matcher.type().requireValue().equals(PI)) {
                    spec = new RadioPlayerPiFeedSpec(matcher.service().requireValue(), matcher.date().requireValue());
                } else if (matcher.type().requireValue().equals(OD)) {
                    DateTime since = matcher.date().requireValue().toDateTimeAtStartOfDay().minusHours(2);
                    spec = new RadioPlayerOdFeedSpec(matcher.service().requireValue(), matcher.date().requireValue(), Optional.of(since), odUriResolver.getServiceToUrisMapSince(since).get(matcher.service().requireValue()));
                } else {
                    throw new IllegalArgumentException("Unknown file type");
                }
                
                RadioPlayerFeedCompiler.valueOf(publisher, feedType).compileFeedFor(spec, response.getOutputStream());
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
