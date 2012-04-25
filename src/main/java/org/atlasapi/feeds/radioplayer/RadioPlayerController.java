package org.atlasapi.feeds.radioplayer;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RadioPlayerController {

    @RequestMapping("feeds/ukradioplayer/{filename}.xml")
    public void xmlForFilename(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {

        RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(filename);

        if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {

            FileType feedType = matcher.type().requireValue();
            try {
                RadioPlayerFeedCompiler.valueOf(feedType).compileFeedFor(new RadioPlayerPiFeedSpec(matcher.service().requireValue(), matcher.date().requireValue()), response.getOutputStream());
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
